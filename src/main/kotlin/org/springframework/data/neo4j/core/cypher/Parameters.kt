/*
 * Copyright 2011-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.cypher

// A couple of extension methods that escapes $parameterNames inside multiline strings.
// See ParameterTest.kt for an example how to use them.

/**
 * Extension on [String] returning the string itself prefixed with an escaped `$`.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
fun String.asParam() = "\$" + this

/**
 * Extension on [String]'s companion object returning the string passed to it prefixed with an escaped `$`.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
infix fun String.Companion.asParam(s: String) = "\$" + s
