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
package com.vaadin.browserless

import kotlin.reflect.KClass
import kotlin.test.assertFailsWith
import com.vaadin.browserless.internal.allViews
import kotlin.test.expect

/**
 * Expects that [actual] list of objects matches [expected] list of objects. Fails otherwise.
 */
fun <T> expectList(vararg expected: T, actual: ()->List<T>) {
    expect(expected.toList(), actual)
}


/**
 * Expects that given block fails with an exception of type [clazz] (or its subtype).
 *
 * Note that this is different from [assertFailsWith] since this function
 * also asserts on [Throwable.message].
 * @param expectMessage regular expression which the [Throwable.message] must match.
 * @throws AssertionError if the block completed successfully or threw some other exception.
 * @return the exception thrown, so that you can assert on it.
 */
fun <T: Throwable> expectThrows(clazz: KClass<out T>, expectMessage: Regex, block: ()->Unit): T {
    // tests for this function are present in the dynatest-engine project
    val ex = assertFailsWith(clazz, block)
    if (!(ex.message ?: "").contains(expectMessage)) {
        throw AssertionError("${clazz.javaObjectType.name} message: Expected '$expectMessage' but was '${ex.message}'", ex)
    }
    return ex
}

/**
 * Expects that given block fails with an exception of type [T] (or its subtype).
 *
 * Note that this is different from [assertFailsWith] since this function
 * also asserts on [Throwable.message].
 * @param expectMessage regular expression which the [Throwable.message] must match.
 * @throws AssertionError if the block completed successfully or threw some other exception.
 * @return the exception thrown, so that you can assert on it.
 */
inline fun <reified T: Throwable> expectThrows(expectMessage: Regex, noinline block: ()->Unit): T =
    expectThrows(T::class, expectMessage, block)


object TestRoutes {
    val views = allViews;
}
