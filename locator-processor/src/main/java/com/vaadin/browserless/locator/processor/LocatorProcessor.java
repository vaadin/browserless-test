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

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
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
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.SimpleTypeVisitor14;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Annotation processor that walks {@code @Tests}-annotated
 * {@code ComponentTester} subclasses in the compilation unit and emits a
 * sibling {@code *Locator} class for each, plus one or more entry-point
 * interfaces exposing a typed {@code find<ComponentName>()} default method per
 * locator (one interface for core entries and an optional separate one for
 * entries whose target lives in a configured commercial package).
 *
 * <p>
 * Generated source uses fully-qualified type names everywhere to avoid the
 * complexity of import management. The output compiles cleanly, just verbosely.
 *
 * <p>
 * <strong>Internal build tool.</strong> This artifact is not published; it is
 * consumed only by other modules in this repository. The processor options are
 * an internal contract — break them freely if a refactor benefits, no
 * deprecation cycle is owed to end users.
 */
@SupportedAnnotationTypes("com.vaadin.browserless.Tests")
@SupportedOptions({ "locator.commercial.packages", "locator.entrypoint.fqn",
        "locator.commercial.entrypoint.fqn" })
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class LocatorProcessor extends AbstractProcessor {

    private static final String TESTS_FQN = "com.vaadin.browserless.Tests";
    private static final String COMPONENT_TESTER_FQN = "com.vaadin.browserless.ComponentTester";
    private static final String LOCATOR_FQN = "com.vaadin.browserless.locator.Locator";
    private static final String CLICKABLE_FQN = "com.vaadin.browserless.Clickable";
    private static final String OPT_COMMERCIAL_PACKAGES = "locator.commercial.packages";
    private static final String OPT_ENTRYPOINT_FQN = "locator.entrypoint.fqn";
    private static final String OPT_COMMERCIAL_ENTRYPOINT_FQN = "locator.commercial.entrypoint.fqn";
    private static final String DEFAULT_ENTRYPOINT_FQN = "com.vaadin.browserless.locator.GeneratedLocators";
    private static final String DEFAULT_COMMERCIAL_ENTRYPOINT_FQN = "com.vaadin.browserless.locator.GeneratedCommercialLocators";
    private static final List<String> DEFAULT_COMMERCIAL_PACKAGES = List
            .of("com.vaadin.flow.component.charts");

    /**
     * Public methods that we never delegate from the locator. These belong to
     * the {@code ComponentTester} base machinery (the locator provides its own
     * resolution + usability surface) or to the locator's own filter chain.
     * <p>
     * {@code click}, {@code middleClick} and {@code rightClick} are
     * <em>not</em> skipped: a tester override is delegated like any other
     * method, and when no tester in the chain declares them the locator picks
     * them up from its own {@code implements Clickable<C>}. The supertype walk
     * below stops at {@code ComponentTester}, so {@code Clickable}'s
     * interface-level defaults are never harvested as delegates.
     */
    private static final Set<String> METHOD_SKIP_LIST = Set.of("getComponent",
            "isUsable", "setModal", "find", "ensureComponentIsUsable");

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
                processTester(tester);
            } catch (Exception ex) {
                note(Diagnostic.Kind.WARNING, "Locator generation skipped for "
                        + tester.getQualifiedName() + ": " + ex.getMessage());
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
     * Inspect a tester element and emit one locator per {@code @Tests} target
     * (value or fqn). For each target, the locator's tester type variables
     * (past the first) are pinned to concrete types when the target's supertype
     * parameterization fixes them — this turns e.g.
     * {@code getTextField(Class<V>)} into a clean {@code getTextField()} for
     * {@code TextField} (V=String) while still requiring a witness for
     * {@code Grid} (V free per use).
     */
    private void processTester(TypeElement tester) {
        if (!extendsComponentTester(tester)) {
            return;
        }
        if (tester.getModifiers().contains(Modifier.ABSTRACT)) {
            return;
        }

        List<TypeParameterElement> extraTypeParams = tester.getTypeParameters()
                .stream().skip(1).collect(Collectors.toList());

        List<TypeElement> targets = readTestsTargets(tester);
        if (targets.isEmpty()) {
            // Fallback: derive a single target from the tester's bound. Used
            // only for testers that don't declare any @Tests value or fqn.
            Set<String> forwardedNames = extraTypeParams.stream()
                    .map(tp -> tp.getSimpleName().toString())
                    .collect(Collectors.toSet());
            TypeMirror bound = resolveComponentType(tester, forwardedNames);
            if (bound != null && bound.getKind() == TypeKind.DECLARED) {
                TypeElement el = (TypeElement) ((DeclaredType) bound)
                        .asElement();
                targets = List.of(el);
            }
            if (targets.isEmpty()) {
                note(Diagnostic.Kind.NOTE, "No @Tests target for "
                        + tester.getQualifiedName() + "; skipping.");
                return;
            }
        }

        for (TypeElement target : targets) {
            Entry entry = generateLocatorForTarget(tester, target,
                    extraTypeParams);
            if (entry != null) {
                entries.add(entry);
            }
        }
    }

    /**
     * Generate one locator class targeting {@code target}. Extras that get
     * pinned by walking {@code target}'s supertypes for the tester's bound head
     * class are removed from the locator's type parameter list and substituted
     * in method signatures.
     */
    private Entry generateLocatorForTarget(TypeElement tester,
            TypeElement target, List<TypeParameterElement> extraTypeParams) {
        String testerSimple = tester.getSimpleName().toString();
        String testerPkg = processingEnv.getElementUtils().getPackageOf(tester)
                .getQualifiedName().toString();

        String targetPkg = processingEnv.getElementUtils().getPackageOf(target)
                .getQualifiedName().toString();
        String targetSimple = target.getSimpleName().toString();
        String locatorSimple = targetSimple + "Locator";
        // Locator is emitted in the tester's package, not the target's. For
        // shared's own testers tester and target share a package, so this is a
        // no-op. For end-user testers that target a framework component (e.g.
        // a custom MyButtonTester @Tests(Button.class)), this avoids dropping
        // a file into a package the user doesn't own and avoids clashing with
        // the framework-emitted locator of the same name.
        String pkg = testerPkg;

        Map<String, TypeMirror> pinned = pinExtras(tester, target,
                extraTypeParams);

        // Locator's own type parameter list = extras that were not pinned.
        List<TypeParameterElement> freeExtras = extraTypeParams.stream().filter(
                tp -> !pinned.containsKey(tp.getSimpleName().toString()))
                .collect(Collectors.toList());

        // Component type expression for `Locator<C, SELF>`. The target may
        // have its own type parameters; substitute them positionally with the
        // locator's free extras (matches the GridTester / ComboBoxTester
        // pattern where the tester's extra Y maps onto the target's T).
        String componentTypeExpr = renderTargetTypeExpr(target, freeExtras);

        String locatorTypeParamDecl = renderTypeParamDecl(freeExtras);
        String locatorTypeParamUse = renderTypeParamUse(freeExtras);
        String selfType = locatorSimple + locatorTypeParamUse;

        // Substitution map for pinned tester type variables. The first tester
        // variable (the component) maps to the target's parameterized form so
        // the tester is constructed with concrete type arguments.
        Map<String, String> subst = new HashMap<>();
        if (!tester.getTypeParameters().isEmpty()) {
            subst.put(tester.getTypeParameters().get(0).getSimpleName()
                    .toString(), componentTypeExpr);
        }
        for (Map.Entry<String, TypeMirror> e : pinned.entrySet()) {
            subst.put(e.getKey(), typeExpr(e.getValue()));
        }

        // Explicit tester type arguments: concrete value for each tester type
        // parameter. Built from `subst`.
        String testerTypeArgs = "<" + tester.getTypeParameters().stream()
                .map(tp -> subst.getOrDefault(tp.getSimpleName().toString(),
                        tp.getSimpleName().toString()))
                .collect(Collectors.joining(", ")) + ">";
        String testerCtor = testerPkg + "." + testerSimple
                + (tester.getTypeParameters().isEmpty() ? "" : testerTypeArgs);

        // Method delegates: walk the supertype chain so methods declared on
        // intermediate base testers (e.g. HtmlContainerTester.getText()) show
        // up on the locator too. Leaf overrides win on signature collision.
        // Stops at ComponentTester so its base-machinery members (and the
        // Clickable interface defaults it inherits) are not delegated.
        StringBuilder methodSrc = new StringBuilder();
        Types types = processingEnv.getTypeUtils();
        TypeElement componentTesterEl = processingEnv.getElementUtils()
                .getTypeElement(COMPONENT_TESTER_FQN);
        DeclaredType testerType = (DeclaredType) tester.asType();
        LinkedHashMap<String, ExecutableElement> inherited = new LinkedHashMap<>();
        collectDelegateMethods(tester, componentTesterEl, inherited);
        for (ExecutableElement m : inherited.values()) {
            ExecutableType resolved = (ExecutableType) types
                    .asMemberOf(testerType, m);
            methodSrc.append(renderDelegate(m, resolved, testerCtor, subst));
        }

        // Constructor: takes Class<V> witnesses only for the free extras. The
        // raw-cast in renderSuperArg fires only when the target has its own
        // type parameters; that's the single line that needs the unchecked
        // /rawtypes suppression. Scope the annotation to the constructor so
        // genuine warnings elsewhere in the generated class still surface.
        String ctor;
        String superArg = renderSuperArg(target, freeExtras);
        boolean needsRawCast = !target.getTypeParameters().isEmpty();
        String ctorAnno = needsRawCast
                ? "    @SuppressWarnings({\"unchecked\", \"rawtypes\"})\n"
                : "";
        if (freeExtras.isEmpty()) {
            ctor = ctorAnno + "    public " + locatorSimple + "() {\n"
                    + "        super(" + superArg + ");\n" + "    }\n";
        } else {
            String params = freeExtras.stream()
                    .map(tp -> "java.lang.Class<" + tp.getSimpleName() + "> "
                            + decap(tp.getSimpleName().toString()) + "Type")
                    .collect(Collectors.joining(", "));
            ctor = ctorAnno + "    public " + locatorSimple + "(" + params
                    + ") {\n" + "        super(" + superArg + ");\n"
                    + "    }\n";
        }

        String fqn = pkg + "." + locatorSimple;
        try {
            JavaFileObject jfo = processingEnv.getFiler().createSourceFile(fqn,
                    tester);
            try (Writer w = jfo.openWriter();
                    PrintWriter out = new PrintWriter(w)) {
                out.println(
                        "/* Generated by LocatorProcessor. Do not edit. */");
                out.println("package " + pkg + ";");
                out.println();
                out.print(renderClassJavadoc(target, tester));
                out.println("@javax.annotation.processing.Generated(\""
                        + LocatorProcessor.class.getName() + "\")");
                out.println("public class " + locatorSimple
                        + locatorTypeParamDecl + " extends " + LOCATOR_FQN + "<"
                        + componentTypeExpr + ", " + selfType + "> implements "
                        + CLICKABLE_FQN + "<" + componentTypeExpr + "> {");
                out.println();
                out.println(ctor);
                out.println("    @Override");
                out.println("    public " + componentTypeExpr
                        + " getComponent() { return component(); }");
                out.println();
                out.println(
                        "    @Override public void ensureComponentIsUsable() {");
                out.println("        new " + testerCtor + "(component())"
                        + ".ensureComponentIsUsable();");
                out.println("    }");
                out.println();
                out.print(methodSrc.toString());
                out.println("}");
            }
        } catch (Exception ioe) {
            note(Diagnostic.Kind.ERROR,
                    "Failed to write " + fqn + ": " + ioe.getMessage());
            return null;
        }

        String entryMethodName = "find" + targetSimple;
        return new Entry(pkg, targetPkg, locatorSimple, locatorTypeParamDecl,
                locatorTypeParamUse, freeExtras, entryMethodName);
    }

    /**
     * Read the {@code @Tests} annotation on the tester, returning the targets
     * listed in {@code value()} together with classes resolved from
     * {@code fqn()}. Both forms are supported because Vaadin testers use a mix.
     */
    @SuppressWarnings("unchecked")
    private List<TypeElement> readTestsTargets(TypeElement tester) {
        List<TypeElement> result = new ArrayList<>();
        for (AnnotationMirror am : tester.getAnnotationMirrors()) {
            if (!am.getAnnotationType().toString().equals(TESTS_FQN)) {
                continue;
            }
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am
                    .getElementValues().entrySet()) {
                String name = entry.getKey().getSimpleName().toString();
                if (name.equals("value")) {
                    List<? extends AnnotationValue> list = (List<? extends AnnotationValue>) entry
                            .getValue().getValue();
                    for (AnnotationValue v : list) {
                        TypeMirror tm = (TypeMirror) v.getValue();
                        if (tm.getKind() == TypeKind.DECLARED) {
                            result.add((TypeElement) ((DeclaredType) tm)
                                    .asElement());
                        }
                    }
                } else if (name.equals("fqn")) {
                    List<? extends AnnotationValue> list = (List<? extends AnnotationValue>) entry
                            .getValue().getValue();
                    for (AnnotationValue v : list) {
                        String fqn = (String) v.getValue();
                        TypeElement te = processingEnv.getElementUtils()
                                .getTypeElement(fqn);
                        if (te != null) {
                            result.add(te);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * For each tester extra (the type variables past the first), walk the
     * target's supertype chain to find the tester's bound head class (e.g.
     * {@code TextFieldBase} or {@code Grid}). The position of the extra in the
     * tester's bound determines which type argument on the target's
     * parameterization to pin against.
     */
    private Map<String, TypeMirror> pinExtras(TypeElement tester,
            TypeElement target, List<TypeParameterElement> extraTypeParams) {
        Map<String, TypeMirror> pinned = new HashMap<>();
        if (extraTypeParams.isEmpty() || tester.getTypeParameters().isEmpty()) {
            return pinned;
        }
        TypeMirror firstBound = tester.getTypeParameters().get(0).getBounds()
                .get(0);
        if (firstBound.getKind() != TypeKind.DECLARED) {
            return pinned;
        }
        DeclaredType firstBoundDt = (DeclaredType) firstBound;
        TypeMirror boundHeadErasure = processingEnv.getTypeUtils()
                .erasure(firstBoundDt);

        // Position of each extra in the tester's bound type args.
        Map<String, Integer> extraPositions = new HashMap<>();
        List<? extends TypeMirror> boundArgs = firstBoundDt.getTypeArguments();
        for (int i = 0; i < boundArgs.size(); i++) {
            TypeMirror arg = boundArgs.get(i);
            if (arg.getKind() == TypeKind.TYPEVAR) {
                String name = ((TypeVariable) arg).asElement().getSimpleName()
                        .toString();
                for (TypeParameterElement tp : extraTypeParams) {
                    if (tp.getSimpleName().contentEquals(name)) {
                        extraPositions.put(name, i);
                    }
                }
            }
        }

        // Find the target's parameterization of the bound head class.
        TypeMirror targetAsHead = findInstanceOf(target.asType(),
                boundHeadErasure);
        if (targetAsHead == null
                || targetAsHead.getKind() != TypeKind.DECLARED) {
            return pinned;
        }
        DeclaredType targetAsHeadDt = (DeclaredType) targetAsHead;
        List<? extends TypeMirror> targetArgs = targetAsHeadDt
                .getTypeArguments();

        for (TypeParameterElement extra : extraTypeParams) {
            String name = extra.getSimpleName().toString();
            Integer pos = extraPositions.get(name);
            if (pos == null || pos >= targetArgs.size()) {
                continue;
            }
            TypeMirror actual = targetArgs.get(pos);
            if (actual.getKind() == TypeKind.TYPEVAR) {
                continue; // not concrete — leave free
            }
            if (actual.getKind() == TypeKind.DECLARED
                    || actual.getKind() == TypeKind.ARRAY) {
                pinned.put(name, actual);
            }
        }
        return pinned;
    }

    private TypeMirror findInstanceOf(TypeMirror tm, TypeMirror targetErasure) {
        if (tm == null || tm.getKind() != TypeKind.DECLARED) {
            return null;
        }
        if (processingEnv.getTypeUtils().isSameType(
                processingEnv.getTypeUtils().erasure(tm), targetErasure)) {
            return tm;
        }
        for (TypeMirror sup : processingEnv.getTypeUtils()
                .directSupertypes(tm)) {
            TypeMirror result = findInstanceOf(sup, targetErasure);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /**
     * Render the target as a fully-qualified type expression. If the target
     * declares type parameters of its own, substitute them positionally with
     * the locator's free extras (which is the right thing for {@code Grid<T>}
     * and {@code ComboBox<T>} where the tester forwards its row/value type).
     */
    private String renderTargetTypeExpr(TypeElement target,
            List<TypeParameterElement> freeExtras) {
        String fqn = target.getQualifiedName().toString();
        List<? extends TypeParameterElement> targetTps = target
                .getTypeParameters();
        if (targetTps.isEmpty()) {
            return fqn;
        }
        StringBuilder sb = new StringBuilder(fqn);
        sb.append('<');
        for (int i = 0; i < targetTps.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            if (i < freeExtras.size()) {
                sb.append(freeExtras.get(i).getSimpleName());
            } else {
                sb.append('?');
            }
        }
        sb.append('>');
        return sb.toString();
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

    /**
     * Walk the tester's superclass chain (stopping at {@code ComponentTester}
     * and {@code Object}), collecting methods eligible for delegation. Leaf
     * classes are visited first, so an inherited method whose erased signature
     * matches a leaf override is skipped — the leaf's version wins.
     */
    private void collectDelegateMethods(TypeElement type,
            TypeElement componentTesterEl,
            LinkedHashMap<String, ExecutableElement> collected) {
        if (type == null
                || type.getQualifiedName().contentEquals("java.lang.Object")) {
            return;
        }
        Types types = processingEnv.getTypeUtils();
        if (componentTesterEl != null
                && types.isSameType(types.erasure(type.asType()),
                        types.erasure(componentTesterEl.asType()))) {
            return;
        }
        for (Element member : type.getEnclosedElements()) {
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
            collected.putIfAbsent(erasedSignatureKey(m), m);
        }
        TypeMirror sup = type.getSuperclass();
        if (sup != null && sup.getKind() == TypeKind.DECLARED) {
            collectDelegateMethods(
                    (TypeElement) ((DeclaredType) sup).asElement(),
                    componentTesterEl, collected);
        }
    }

    private String erasedSignatureKey(ExecutableElement m) {
        StringBuilder sb = new StringBuilder();
        sb.append(m.getSimpleName()).append('(');
        List<? extends VariableElement> params = m.getParameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(processingEnv.getTypeUtils()
                    .erasure(params.get(i).asType()));
        }
        sb.append(')');
        return sb.toString();
    }

    private String renderDelegate(ExecutableElement m, ExecutableType resolved,
            String testerCtor, Map<String, String> subst) {
        StringBuilder sb = new StringBuilder();
        sb.append(renderJavadoc(m));
        // Method type parameters
        if (!m.getTypeParameters().isEmpty()) {
            sb.append("    public <");
            sb.append(m.getTypeParameters().stream()
                    .map(tp -> renderTypeParamWithSubst(tp, subst))
                    .collect(Collectors.joining(", ")));
            sb.append("> ");
        } else {
            sb.append("    public ");
        }
        sb.append(typeExpr(resolved.getReturnType(), subst)).append(' ')
                .append(m.getSimpleName()).append('(');
        // Parameters: take names from the element, types from the resolved
        // ExecutableType so type variables inherited from intermediate base
        // testers are rebound through the leaf tester's declaration.
        List<? extends VariableElement> params = m.getParameters();
        List<? extends TypeMirror> resolvedParams = resolved
                .getParameterTypes();
        StringBuilder paramNames = new StringBuilder();
        for (int i = 0; i < params.size(); i++) {
            VariableElement p = params.get(i);
            TypeMirror pt = resolvedParams.get(i);
            String pType = m.isVarArgs() && i == params.size() - 1
                    ? varargTypeExpr(pt, subst)
                    : typeExpr(pt, subst);
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
        if (!resolved.getThrownTypes().isEmpty()) {
            sb.append(" throws ");
            sb.append(resolved.getThrownTypes().stream()
                    .map(t -> typeExpr(t, subst))
                    .collect(Collectors.joining(", ")));
        }
        sb.append(" {\n");
        // Body
        sb.append("        ");
        if (resolved.getReturnType().getKind() != TypeKind.VOID) {
            sb.append("return ");
        }
        sb.append("new ").append(testerCtor).append("(component()).");
        if (!m.getTypeParameters().isEmpty()) {
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

    private String renderClassJavadoc(TypeElement target, TypeElement tester) {
        String targetFqn = target.getQualifiedName().toString();
        String testerFqn = tester.getQualifiedName().toString();
        StringBuilder sb = new StringBuilder();
        sb.append("/**\n");
        sb.append(" * Generated locator for {@link ").append(targetFqn)
                .append("}, derived from\n");
        sb.append(" * {@link ").append(testerFqn)
                .append("}. Filter steps are inherited from\n");
        sb.append(" * {@link ").append(LOCATOR_FQN)
                .append("}; action methods delegate to a fresh tester\n");
        sb.append(
                " * around the resolved component, so behavioral changes belong on the\n");
        sb.append(" * tester, not here.\n");
        sb.append(" */\n");
        return sb.toString();
    }

    private String renderJavadoc(ExecutableElement m) {
        TypeElement declaring = (TypeElement) m.getEnclosingElement();
        String linkRef = buildLinkRef(m, declaring);
        String doc = processingEnv.getElementUtils().getDocComment(m);

        StringBuilder sb = new StringBuilder();
        sb.append("    /**\n");
        if (doc != null && !doc.isBlank()) {
            // getDocComment strips the leading "*" markers but keeps a single
            // leading space on each line; drop it before re-emitting.
            String[] raw = doc.stripTrailing().split("\\R", -1);
            String[] lines = new String[raw.length];
            int firstTag = raw.length;
            for (int i = 0; i < raw.length; i++) {
                lines[i] = raw[i].startsWith(" ") ? raw[i].substring(1)
                        : raw[i];
                if (firstTag == raw.length && lines[i].startsWith("@")) {
                    firstTag = i;
                }
            }
            int descEnd = firstTag;
            while (descEnd > 0 && lines[descEnd - 1].isEmpty()) {
                descEnd--;
            }
            for (int i = 0; i < descEnd; i++) {
                appendDocLine(sb, lines[i]);
            }
            if (descEnd > 0) {
                sb.append("     *\n");
            }
            sb.append("     * Javadoc copied from {@link ").append(linkRef)
                    .append("}.\n");
            if (firstTag < raw.length) {
                sb.append("     *\n");
                for (int i = firstTag; i < raw.length; i++) {
                    appendDocLine(sb, lines[i]);
                }
            }
        } else {
            sb.append("     * Delegates to {@link ").append(linkRef)
                    .append("}.\n");
        }
        sb.append("     */\n");
        return sb.toString();
    }

    private void appendDocLine(StringBuilder sb, String line) {
        if (line.isEmpty()) {
            sb.append("     *\n");
        } else {
            sb.append("     * ").append(line).append('\n');
        }
    }

    private String buildLinkRef(ExecutableElement m, TypeElement tester) {
        String testerFqn = tester.getQualifiedName().toString();
        String params = m
                .getParameters().stream().map(p -> processingEnv.getTypeUtils()
                        .erasure(p.asType()).toString())
                .collect(Collectors.joining(","));
        return testerFqn + "#" + m.getSimpleName() + "(" + params + ")";
    }

    private String renderTypeParamWithSubst(TypeParameterElement tp,
            Map<String, String> subst) {
        StringBuilder sb = new StringBuilder();
        sb.append(tp.getSimpleName());
        List<? extends TypeMirror> bounds = tp.getBounds();
        if (!bounds.isEmpty()
                && !bounds.get(0).toString().equals("java.lang.Object")) {
            sb.append(" extends ");
            sb.append(bounds.stream().map(t -> typeExpr(t, subst))
                    .collect(Collectors.joining(" & ")));
        }
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
     * locator. For non-generic targets this is just {@code Target.class}; for
     * targets that have type parameters not all pinned by the locator, we use a
     * raw class literal cast (the cast is compile-time only).
     */
    private String renderSuperArg(TypeElement target,
            List<TypeParameterElement> freeExtras) {
        String fqn = target.getQualifiedName().toString();
        if (target.getTypeParameters().isEmpty() && freeExtras.isEmpty()) {
            return fqn + ".class";
        }
        if (target.getTypeParameters().isEmpty()) {
            // Target itself is non-generic but we are still parameterizing the
            // locator with free extras. Plain class literal still works.
            return fqn + ".class";
        }
        return "(java.lang.Class) " + fqn + ".class";
    }

    /**
     * Render a type mirror, substituting any tester-private type variables with
     * their pinned concrete types via {@code subst}.
     */
    private String typeExpr(TypeMirror tm, Map<String, String> subst) {
        if (subst == null || subst.isEmpty()) {
            return tm.toString();
        }
        return new SubstitutingTypeRenderer(subst).visit(tm);
    }

    private String typeExpr(TypeMirror tm) {
        return tm.toString();
    }

    private String varargTypeExpr(TypeMirror tm, Map<String, String> subst) {
        String s = typeExpr(tm, subst);
        if (s.endsWith("[]")) {
            return s.substring(0, s.length() - 2) + "...";
        }
        return s;
    }

    /**
     * Type renderer that substitutes tester-private type variables with their
     * pinned concrete types. Used so a method like {@code setValue(V)} on the
     * tester becomes {@code setValue(String)} on a {@code TextField} locator.
     */
    private static final class SubstitutingTypeRenderer
            extends SimpleTypeVisitor14<String, Void> {

        private final Map<String, String> subst;

        SubstitutingTypeRenderer(Map<String, String> subst) {
            this.subst = subst;
        }

        @Override
        public String visitTypeVariable(TypeVariable t, Void unused) {
            String name = t.asElement().getSimpleName().toString();
            return subst.getOrDefault(name, name);
        }

        @Override
        public String visitDeclared(DeclaredType t, Void unused) {
            StringBuilder sb = new StringBuilder();
            sb.append(((TypeElement) t.asElement()).getQualifiedName());
            if (!t.getTypeArguments().isEmpty()) {
                sb.append('<');
                sb.append(t.getTypeArguments().stream()
                        .map(arg -> arg.accept(this, null))
                        .collect(Collectors.joining(", ")));
                sb.append('>');
            }
            return sb.toString();
        }

        @Override
        public String visitArray(javax.lang.model.type.ArrayType t, Void v) {
            return t.getComponentType().accept(this, null) + "[]";
        }

        @Override
        public String visitWildcard(javax.lang.model.type.WildcardType t,
                Void v) {
            StringBuilder sb = new StringBuilder("?");
            if (t.getExtendsBound() != null) {
                sb.append(" extends ")
                        .append(t.getExtendsBound().accept(this, null));
            }
            if (t.getSuperBound() != null) {
                sb.append(" super ")
                        .append(t.getSuperBound().accept(this, null));
            }
            return sb.toString();
        }

        @Override
        protected String defaultAction(TypeMirror t, Void v) {
            return t.toString();
        }
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
        List<String> commercialPrefixes = readCommercialPrefixes();
        List<Entry> core = new ArrayList<>();
        List<Entry> commercial = new ArrayList<>();
        for (Entry e : entries) {
            if (isCommercial(e, commercialPrefixes)) {
                commercial.add(e);
            } else {
                core.add(e);
            }
        }
        if (!core.isEmpty()) {
            writeEntryPointIfConfigured(OPT_ENTRYPOINT_FQN,
                    DEFAULT_ENTRYPOINT_FQN, core);
        }
        if (!commercial.isEmpty()) {
            writeEntryPointIfConfigured(OPT_COMMERCIAL_ENTRYPOINT_FQN,
                    DEFAULT_COMMERCIAL_ENTRYPOINT_FQN, commercial);
        }
    }

    /**
     * Resolve the entry-point FQN from a processor option, falling back to the
     * framework default. When the option is unset AND the default FQN already
     * exists on the classpath, the interface is not written — this is the
     * "end-user build pulling in shared.jar" scenario, where overwriting the
     * framework interface would lose all the upstream entry methods. We emit a
     * clear warning so the user knows to set the option.
     */
    private void writeEntryPointIfConfigured(String optionKey,
            String defaultFqn, List<Entry> entriesToWrite) {
        String configured = processingEnv.getOptions().get(optionKey);
        String fqn = configured != null && !configured.isBlank() ? configured
                : defaultFqn;
        boolean usingDefault = configured == null || configured.isBlank();
        if (usingDefault && processingEnv.getElementUtils()
                .getTypeElement(fqn) != null) {
            note(Diagnostic.Kind.WARNING,
                    fqn + " is already on the classpath; skipping generation."
                            + " To emit a project-specific entry-point"
                            + " interface, set -A" + optionKey
                            + "=<your.package.YourLocators>.");
            return;
        }
        int lastDot = fqn.lastIndexOf('.');
        if (lastDot < 0) {
            note(Diagnostic.Kind.ERROR,
                    "Entry-point FQN must be qualified: " + fqn);
            return;
        }
        writeInterface(fqn.substring(0, lastDot), fqn.substring(lastDot + 1),
                entriesToWrite);
    }

    private List<String> readCommercialPrefixes() {
        String opt = processingEnv.getOptions().get(OPT_COMMERCIAL_PACKAGES);
        if (opt == null || opt.isBlank()) {
            return DEFAULT_COMMERCIAL_PACKAGES;
        }
        return Arrays.stream(opt.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }

    private boolean isCommercial(Entry e, List<String> prefixes) {
        // "Commercial" is a property of the target component (Chart lives in
        // a commercial module), not of the tester that wraps it. Match
        // against the target's package so a user-written commercial tester
        // located in their own package is still routed correctly.
        return prefixes.stream().anyMatch(
                p -> e.targetPkg.equals(p) || e.targetPkg.startsWith(p + "."));
    }

    private void writeInterface(String pkg, String simpleName,
            List<Entry> interfaceEntries) {
        String fqn = pkg + "." + simpleName;
        try {
            JavaFileObject jfo = processingEnv.getFiler().createSourceFile(fqn);
            try (Writer w = jfo.openWriter();
                    PrintWriter out = new PrintWriter(w)) {
                out.println(
                        "/* Generated by LocatorProcessor. Do not edit. */");
                out.println("package " + pkg + ";");
                out.println();
                out.println("/**");
                out.println(
                        " * Generated mixin: one typed {@code find<Component>()} entry per registered");
                out.println(
                        " * {@code ComponentTester}, each returning a fresh {@code *Locator} ready");
                out.println(" * for chaining queries and actions.");
                out.println(" *");
                out.println(
                        " * <p>Not consumed directly — extend it from your locator context mixin");
                out.println(
                        " * (the one that implements {@link #activateLocatorContext()}).");
                out.println(" */");
                out.println("@javax.annotation.processing.Generated(\""
                        + LocatorProcessor.class.getName() + "\")");
                out.println("public interface " + simpleName + " {");
                out.println();
                out.println("    /**");
                out.println(
                        "     * Hook for context-bound implementations to install Vaadin thread-locals");
                out.println("     * before a locator is built.");
                out.println("     */");
                out.println("    void activateLocatorContext();");
                out.println();
                // Deduplicate by entry method signature; if two testers map to
                // the same component simple name we keep the first.
                TreeMap<String, Entry> unique = new TreeMap<>();
                Set<String> seenMethods = new LinkedHashSet<>();
                for (Entry e : interfaceEntries) {
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
                                    + "> "
                                    + decap(tp.getSimpleName().toString())
                                    + "Type")
                            .collect(Collectors.joining(", "));
                    String passArgs = e.extraTypeParams.stream().map(
                            tp -> decap(tp.getSimpleName().toString()) + "Type")
                            .collect(Collectors.joining(", "));
                    out.print(renderEntryJavadoc(e));
                    out.println("    default " + declTp
                            + (declTp.isEmpty() ? "" : " ") + retType + " "
                            + e.entryMethodName + "(" + params + ") {");
                    out.println("        activateLocatorContext();");
                    out.println("        return new " + locatorFqn
                            + diamond(e.extraTypeParams) + "(" + passArgs
                            + ");");
                    out.println("    }");
                    out.println();
                }
                out.println("}");
            }
        } catch (Exception ex) {
            note(Diagnostic.Kind.ERROR,
                    "Failed to write " + fqn + ": " + ex.getMessage());
        }
    }

    private String renderEntryJavadoc(Entry e) {
        String locatorFqn = e.pkg + "." + e.locatorSimple;
        String componentSimple = e.locatorSimple.endsWith("Locator")
                ? e.locatorSimple.substring(0,
                        e.locatorSimple.length() - "Locator".length())
                : e.locatorSimple;
        String componentFqn = e.targetPkg + "." + componentSimple;

        StringBuilder sb = new StringBuilder();
        sb.append("    /**\n");
        sb.append("     * Returns a locator for {@link ").append(componentFqn)
                .append("} components.\n");
        if (!e.extraTypeParams.isEmpty()) {
            sb.append("     *\n");
            for (TypeParameterElement tp : e.extraTypeParams) {
                sb.append("     * @param <").append(tp.getSimpleName())
                        .append(">\n");
                sb.append(
                        "     *            type parameter forwarded to the locator\n");
            }
            for (TypeParameterElement tp : e.extraTypeParams) {
                String name = decap(tp.getSimpleName().toString()) + "Type";
                sb.append("     * @param ").append(name).append('\n');
                sb.append("     *            {@link Class} witness for {@code ")
                        .append(tp.getSimpleName()).append("}\n");
            }
        }
        sb.append("     *\n");
        sb.append("     * @return a new {@link ").append(locatorFqn)
                .append("}\n");
        sb.append("     */\n");
        return sb.toString();
    }

    private void note(Diagnostic.Kind kind, String msg) {
        processingEnv.getMessager().printMessage(kind,
                "[LocatorProcessor] " + msg);
    }

    private static String decap(String s) {
        if (s.isEmpty()) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private record Entry(String pkg, String targetPkg, String locatorSimple,
            String locatorTypeParamDecl, String locatorTypeParamUse,
            List<TypeParameterElement> extraTypeParams,
            String entryMethodName) {
    }
}
