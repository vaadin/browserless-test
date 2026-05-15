/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.browserless.locator.processor;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.SimpleTypeVisitor14;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * Annotation processor that walks {@code @Tests}-annotated
 * {@code ComponentTester} subclasses in the compilation unit and emits a
 * sibling {@code *Locator} class for each, plus a single
 * {@code GeneratedLocators} interface that exposes a typed
 * {@code get<ComponentName>()} entry point per locator.
 *
 * <p>
 * Generated source uses fully-qualified type names everywhere to avoid the
 * complexity of import management. The output compiles cleanly, just verbosely.
 */
@SupportedAnnotationTypes("com.vaadin.browserless.Tests")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class LocatorProcessor extends AbstractProcessor {

    private static final String TESTS_FQN = "com.vaadin.browserless.Tests";
    private static final String COMPONENT_TESTER_FQN = "com.vaadin.browserless.ComponentTester";
    private static final String LOCATOR_FQN = "com.vaadin.browserless.locator.Locator";
    private static final String CLICKABLE_FQN = "com.vaadin.browserless.Clickable";

    /**
     * Public methods declared on {@code ComponentTester} or {@code Clickable}
     * that we never delegate from the locator. Either provided by the
     * locator/clickable base directly or replaced by the locator's own filter
     * chain.
     */
    private static final Set<String> METHOD_SKIP_LIST = Set.of("getComponent",
            "isUsable", "setModal", "find", "ensureComponentIsUsable", "click",
            "middleClick", "rightClick");

    /** Collected entries used to emit {@code GeneratedLocators}. */
    private final List<Entry> entries = new ArrayList<>();
    private boolean wroteEntryInterface = false;

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        TypeElement testsAnno = processingEnv.getElementUtils()
                .getTypeElement(TESTS_FQN);
        if (testsAnno == null) {
            return false;
        }

        for (Element e : roundEnv.getElementsAnnotatedWith(testsAnno)) {
            if (e.getKind() != ElementKind.CLASS) {
                continue;
            }
            TypeElement tester = (TypeElement) e;
            try {
                Entry entry = processTester(tester);
                if (entry != null) {
                    entries.add(entry);
                }
            } catch (Exception ex) {
                note(Diagnostic.Kind.WARNING,
                        "Locator generation skipped for "
                                + tester.getQualifiedName() + ": "
                                + ex.getMessage());
            }
        }

        // Emit the entry-point interface once we have collected entries. Doing
        // it in the first non-final round (rather than at processingOver)
        // ensures the generated file is compiled in this build.
        if (!entries.isEmpty() && !wroteEntryInterface) {
            writeEntryPointInterface();
            wroteEntryInterface = true;
        }
        return false;
    }

    /**
     * Inspect a tester element and emit a locator source file. Returns an
     * {@link Entry} describing the locator for later inclusion in
     * {@code GeneratedLocators}.
     */
    private Entry processTester(TypeElement tester) {
        if (!extendsComponentTester(tester)) {
            return null;
        }
        if (tester.getModifiers().contains(Modifier.ABSTRACT)) {
            return null;
        }

        // Tester type parameters past the first one (which binds the component
        // type) are forwarded onto the locator as-is. This covers Grid<V>,
        // ComboBox<V>, CheckboxGroup<V>, etc.
        List<TypeParameterElement> extraTypeParams = tester.getTypeParameters()
                .stream().skip(1).collect(Collectors.toList());
        Set<String> forwardedNames = extraTypeParams.stream()
                .map(tp -> tp.getSimpleName().toString())
                .collect(Collectors.toSet());

        TypeMirror componentTypeMirror = resolveComponentType(tester,
                forwardedNames);
        if (componentTypeMirror == null) {
            note(Diagnostic.Kind.NOTE,
                    "Cannot derive component type for "
                            + tester.getQualifiedName() + "; skipping.");
            return null;
        }

        String pkg = processingEnv.getElementUtils().getPackageOf(tester)
                .getQualifiedName().toString();
        String testerSimple = tester.getSimpleName().toString();
        String locatorSimple = testerSimple.endsWith("Tester")
                ? testerSimple.substring(0,
                        testerSimple.length() - "Tester".length()) + "Locator"
                : testerSimple + "Locator";

        String componentTypeExpr = typeExpr(componentTypeMirror);
        // Type parameter declaration on the locator class itself
        String locatorTypeParamDecl = renderTypeParamDecl(extraTypeParams);
        String locatorTypeParamUse = renderTypeParamUse(extraTypeParams);
        String selfType = locatorSimple + locatorTypeParamUse;

        // Method delegates
        StringBuilder methodSrc = new StringBuilder();
        for (Element member : tester.getEnclosedElements()) {
            if (member.getKind() != ElementKind.METHOD) {
                continue;
            }
            ExecutableElement m = (ExecutableElement) member;
            if (!m.getModifiers().contains(Modifier.PUBLIC)) {
                continue;
            }
            if (m.getModifiers().contains(Modifier.STATIC)) {
                continue;
            }
            if (METHOD_SKIP_LIST.contains(m.getSimpleName().toString())) {
                continue;
            }
            methodSrc.append(renderDelegate(m, testerSimple, pkg,
                    componentTypeExpr, tester.getTypeParameters()));
        }

        // Constructor: pass value-type witnesses through to the tester (for
        // generic locators) and call super with the (raw) component class.
        String ctor;
        String superArg = renderSuperArg(componentTypeMirror, extraTypeParams);
        if (extraTypeParams.isEmpty()) {
            ctor = "    public " + locatorSimple + "() {\n"
                    + "        super(" + superArg + ");\n"
                    + "    }\n";
        } else {
            String params = extraTypeParams.stream()
                    .map(tp -> "java.lang.Class<" + tp.getSimpleName()
                            + "> " + decap(tp.getSimpleName().toString())
                            + "Type")
                    .collect(Collectors.joining(", "));
            ctor = "    public " + locatorSimple + "(" + params + ") {\n"
                    + "        super(" + superArg + ");\n"
                    + "    }\n";
        }

        // Emit source file
        String fqn = pkg + "." + locatorSimple;
        try {
            JavaFileObject jfo = processingEnv.getFiler()
                    .createSourceFile(fqn, tester);
            try (Writer w = jfo.openWriter();
                    PrintWriter out = new PrintWriter(w)) {
                out.println("/* Generated by LocatorProcessor. Do not edit. */");
                out.println("package " + pkg + ";");
                out.println();
                out.println("@javax.annotation.processing.Generated(\""
                        + LocatorProcessor.class.getName() + "\")");
                out.println(
                        "@SuppressWarnings({\"unchecked\", \"rawtypes\"})");
                out.println("public class " + locatorSimple + locatorTypeParamDecl
                        + " extends " + LOCATOR_FQN + "<" + componentTypeExpr
                        + ", " + selfType + "> implements " + CLICKABLE_FQN
                        + "<" + componentTypeExpr + "> {");
                out.println();
                out.println(ctor);
                out.println("    @Override");
                out.println("    public " + componentTypeExpr
                        + " getComponent() { return component(); }");
                out.println();
                out.println(
                        "    @Override public void ensureComponentIsUsable() {");
                out.println("        new " + pkg + "." + testerSimple
                        + diamond(tester.getTypeParameters()) + "(component())"
                        + ".ensureComponentIsUsable();");
                out.println("    }");
                out.println();
                out.print(methodSrc.toString());
                out.println("}");
            }
        } catch (Exception ioe) {
            note(Diagnostic.Kind.ERROR, "Failed to write " + fqn + ": "
                    + ioe.getMessage());
            return null;
        }

        // Entry-point method on GeneratedLocators. Derived from the locator's
        // simple name so users always see a stable `get<ComponentName>`
        // even when the component type was erased.
        String entryComponent = locatorSimple.substring(0,
                locatorSimple.length() - "Locator".length());
        String entryMethodName = "get" + entryComponent;
        return new Entry(pkg, locatorSimple, locatorTypeParamDecl,
                locatorTypeParamUse, extraTypeParams, entryMethodName);
    }

    /**
     * Resolve the locator's component type by walking the type hierarchy via
     * {@link javax.lang.model.util.Types#directSupertypes(TypeMirror)}, which
     * substitutes type arguments through intermediate base classes. If the
     * resulting expression references tester-private type variables that are
     * not forwarded to the locator, fall back to the erased component type.
     */
    private TypeMirror resolveComponentType(TypeElement tester,
            Set<String> forwardedNames) {
        TypeMirror componentTesterErasure = processingEnv.getTypeUtils()
                .erasure(processingEnv.getElementUtils()
                        .getTypeElement(COMPONENT_TESTER_FQN).asType());
        TypeMirror found = findComponentTesterArg(tester.asType(),
                componentTesterErasure, new HashSet<>());
        if (found == null) {
            return null;
        }
        if (found.getKind() == TypeKind.TYPEVAR) {
            // ComponentTester<T> where T is a tester type variable — unwrap to
            // T's first bound so we get the actual component class.
            found = ((TypeVariable) found).getUpperBound();
        }
        return erasePrivateTypeVars(tester, found, forwardedNames);
    }

    private TypeMirror findComponentTesterArg(TypeMirror tm,
            TypeMirror componentTesterErasure, Set<String> visited) {
        if (tm == null || tm.getKind() != TypeKind.DECLARED) {
            return null;
        }
        DeclaredType dt = (DeclaredType) tm;
        String key = ((TypeElement) dt.asElement()).getQualifiedName()
                .toString();
        if (!visited.add(key)) {
            return null;
        }
        if (processingEnv.getTypeUtils().isSameType(
                processingEnv.getTypeUtils().erasure(dt),
                componentTesterErasure)) {
            return dt.getTypeArguments().isEmpty() ? null
                    : dt.getTypeArguments().get(0);
        }
        for (TypeMirror sup : processingEnv.getTypeUtils()
                .directSupertypes(dt)) {
            TypeMirror found = findComponentTesterArg(sup,
                    componentTesterErasure, visited);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private TypeMirror erasePrivateTypeVars(TypeElement tester, TypeMirror tm,
            Set<String> forwardedNames) {
        Set<String> testerOwnNames = tester.getTypeParameters().stream()
                .map(p -> p.getSimpleName().toString())
                .collect(Collectors.toSet());
        Set<String> privateNames = new HashSet<>(testerOwnNames);
        privateNames.removeAll(forwardedNames);
        if (privateNames.isEmpty()) {
            return tm;
        }
        boolean[] hit = { false };
        new SimpleTypeVisitor14<Void, Void>() {
            @Override
            public Void visitTypeVariable(TypeVariable t, Void v) {
                if (privateNames
                        .contains(t.asElement().getSimpleName().toString())) {
                    hit[0] = true;
                }
                return null;
            }

            @Override
            public Void visitDeclared(DeclaredType t, Void v) {
                for (TypeMirror arg : t.getTypeArguments()) {
                    arg.accept(this, null);
                }
                return null;
            }
        }.visit(tm);
        if (hit[0]) {
            return processingEnv.getTypeUtils().erasure(tm);
        }
        return tm;
    }

    private boolean extendsComponentTester(TypeElement tester) {
        TypeElement componentTester = processingEnv.getElementUtils()
                .getTypeElement(COMPONENT_TESTER_FQN);
        if (componentTester == null) {
            return false;
        }
        TypeMirror componentTesterErasure = processingEnv.getTypeUtils()
                .erasure(componentTester.asType());
        TypeMirror sup = tester.getSuperclass();
        while (sup != null && sup.getKind() == TypeKind.DECLARED) {
            if (processingEnv.getTypeUtils().isSameType(
                    processingEnv.getTypeUtils().erasure(sup),
                    componentTesterErasure)) {
                return true;
            }
            sup = ((TypeElement) ((DeclaredType) sup).asElement())
                    .getSuperclass();
        }
        return false;
    }

    private String renderDelegate(ExecutableElement m, String testerSimple,
            String pkg, String componentTypeExpr,
            List<? extends TypeParameterElement> testerTypeParams) {
        StringBuilder sb = new StringBuilder();
        // Method type parameters
        if (!m.getTypeParameters().isEmpty()) {
            sb.append("    public <");
            sb.append(m.getTypeParameters().stream()
                    .map(this::renderTypeParam)
                    .collect(Collectors.joining(", ")));
            sb.append("> ");
        } else {
            sb.append("    public ");
        }
        sb.append(typeExpr(m.getReturnType())).append(' ')
                .append(m.getSimpleName()).append('(');
        // Parameters
        List<? extends VariableElement> params = m.getParameters();
        StringBuilder paramNames = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            VariableElement p = params.get(i);
            String pType = m.isVarArgs() && i == params.size() - 1
                    ? varargTypeExpr(p.asType())
                    : typeExpr(p.asType());
            if (i > 0) {
                sb.append(", ");
                paramNames.append(", ");
            }
            sb.append("final ").append(pType).append(' ')
                    .append(p.getSimpleName());
            paramNames.append(p.getSimpleName());
        }
        sb.append(')');
        // Throws clause
        if (!m.getThrownTypes().isEmpty()) {
            sb.append(" throws ");
            sb.append(m.getThrownTypes().stream().map(this::typeExpr)
                    .collect(Collectors.joining(", ")));
        }
        sb.append(" {\n");
        // Body
        String testerCtorArgs = "component()";
        sb.append("        ");
        if (m.getReturnType().getKind() != TypeKind.VOID) {
            sb.append("return ");
        }
        sb.append("new ").append(pkg).append('.').append(testerSimple)
                .append(diamond(testerTypeParams)).append('(')
                .append(testerCtorArgs).append(").");
        if (!m.getTypeParameters().isEmpty()) {
            // Forward method-level type args explicitly so type inference is
            // not required at the delegate call site.
            sb.append('<');
            sb.append(m.getTypeParameters().stream()
                    .map(tp -> tp.getSimpleName().toString())
                    .collect(Collectors.joining(", ")));
            sb.append('>');
        }
        sb.append(m.getSimpleName()).append('(').append(paramNames)
                .append(");\n");
        sb.append("    }\n\n");
        return sb.toString();
    }

    private String renderTypeParam(TypeParameterElement tp) {
        StringBuilder sb = new StringBuilder();
        sb.append(tp.getSimpleName());
        List<? extends TypeMirror> bounds = tp.getBounds();
        if (!bounds.isEmpty()
                && !bounds.get(0).toString().equals("java.lang.Object")) {
            sb.append(" extends ");
            sb.append(bounds.stream().map(this::typeExpr)
                    .collect(Collectors.joining(" & ")));
        }
        return sb.toString();
    }

    private String renderTypeParamDecl(List<TypeParameterElement> tps) {
        if (tps.isEmpty()) {
            return "";
        }
        return "<" + tps.stream().map(this::renderTypeParam)
                .collect(Collectors.joining(", ")) + ">";
    }

    private String renderTypeParamUse(List<TypeParameterElement> tps) {
        if (tps.isEmpty()) {
            return "";
        }
        return "<" + tps.stream().map(t -> t.getSimpleName().toString())
                .collect(Collectors.joining(", ")) + ">";
    }

    private String diamond(List<? extends TypeParameterElement> tps) {
        return tps.isEmpty() ? "" : "<>";
    }

    /**
     * Produces the argument passed to {@code super(...)} when constructing a
     * locator. For raw or non-generic component types this is just
     * {@code ComponentClass.class}. For generic component types we use a raw
     * class literal cast to the parameterized type (no runtime concern — the
     * cast is for the compiler).
     */
    private String renderSuperArg(TypeMirror componentTypeMirror,
            List<TypeParameterElement> extraTypeParams) {
        String erasedExpr = typeExpr(
                processingEnv.getTypeUtils().erasure(componentTypeMirror));
        if (extraTypeParams.isEmpty()) {
            return erasedExpr + ".class";
        }
        // (Class) ComponentClass.class — cast is fine at compile time
        return "(java.lang.Class) " + erasedExpr + ".class";
    }

    private String typeExpr(TypeMirror tm) {
        // TypeMirror.toString() produces a fully-qualified form for declared
        // types, and reuses type variable names verbatim. Good enough for
        // generated code.
        return tm.toString();
    }

    private String varargTypeExpr(TypeMirror tm) {
        // The last vararg parameter type is an ArrayType in the model; convert
        // back to T... form so callers can keep using varargs at the source
        // level.
        String s = tm.toString();
        if (s.endsWith("[]")) {
            return s.substring(0, s.length() - 2) + "...";
        }
        return s;
    }

    private String componentSimpleName(TypeMirror tm) {
        if (tm.getKind() == TypeKind.DECLARED) {
            TypeElement el = (TypeElement) ((DeclaredType) tm).asElement();
            return el.getSimpleName().toString();
        }
        if (tm.getKind() == TypeKind.TYPEVAR) {
            return ((TypeVariable) tm).asElement().getSimpleName().toString();
        }
        return tm.toString();
    }

    private void writeEntryPointInterface() {
        String pkg = "com.vaadin.browserless.locator";
        String simpleName = "GeneratedLocators";
        String fqn = pkg + "." + simpleName;
        try {
            JavaFileObject jfo = processingEnv.getFiler()
                    .createSourceFile(fqn);
            try (Writer w = jfo.openWriter();
                    PrintWriter out = new PrintWriter(w)) {
                out.println(
                        "/* Generated by LocatorProcessor. Do not edit. */");
                out.println("package " + pkg + ";");
                out.println();
                out.println("@javax.annotation.processing.Generated(\""
                        + LocatorProcessor.class.getName() + "\")");
                out.println("public interface " + simpleName + " {");
                out.println();
                out.println("    void activateLocatorContext();");
                out.println();
                // Deduplicate by entry method signature; if two testers map to
                // the same component simple name we keep the first.
                TreeMap<String, Entry> unique = new TreeMap<>();
                Set<String> seenMethods = new LinkedHashSet<>();
                for (Entry e : entries) {
                    String key = e.entryMethodName + "/"
                            + e.extraTypeParams.size();
                    if (seenMethods.add(key)) {
                        unique.put(e.pkg + "." + e.locatorSimple, e);
                    }
                }
                for (Entry e : unique.values()) {
                    String locatorFqn = e.pkg + "." + e.locatorSimple;
                    String declTp = e.locatorTypeParamDecl;
                    String useTp = e.locatorTypeParamUse;
                    String retType = locatorFqn + useTp;
                    String params = e.extraTypeParams.stream()
                            .map(tp -> "java.lang.Class<" + tp.getSimpleName()
                                    + "> " + decap(tp.getSimpleName()
                                            .toString())
                                    + "Type")
                            .collect(Collectors.joining(", "));
                    String passArgs = e.extraTypeParams.stream()
                            .map(tp -> decap(tp.getSimpleName().toString())
                                    + "Type")
                            .collect(Collectors.joining(", "));
                    out.println("    default " + declTp
                            + (declTp.isEmpty() ? "" : " ") + retType + " "
                            + e.entryMethodName + "(" + params + ") {");
                    out.println("        activateLocatorContext();");
                    out.println(
                            "        return new " + locatorFqn + diamond(
                                    e.extraTypeParams) + "(" + passArgs
                                    + ");");
                    out.println("    }");
                    out.println();
                }
                out.println("}");
            }
        } catch (Exception ex) {
            note(Diagnostic.Kind.ERROR,
                    "Failed to write GeneratedLocators: " + ex.getMessage());
        }
    }

    private void note(Diagnostic.Kind kind, String msg) {
        processingEnv.getMessager().printMessage(kind, "[LocatorProcessor] "
                + msg);
    }

    private static String decap(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private record Entry(String pkg, String locatorSimple,
            String locatorTypeParamDecl, String locatorTypeParamUse,
            List<TypeParameterElement> extraTypeParams,
            String entryMethodName) {
    }
}
