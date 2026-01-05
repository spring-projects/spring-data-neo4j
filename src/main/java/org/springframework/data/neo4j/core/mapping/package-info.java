/*
 * Copyright 2011-present the original author or authors.
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
/**
 * <!-- tag::intent[] --> The main mapping framework. This package orchestrates the
 * reading and writing of entities and all tasks related to it. The only public API of
 * this package is the subpackage {@literal callback}, containing the event support. The
 * core package itself has to be considered an internal api, and we don't give any
 * guarantees of API stability. <!-- end::intent[] -->
 * @author Michael J. Simons
 */
@NullMarked
package org.springframework.data.neo4j.core.mapping;

import org.jspecify.annotations.NullMarked;
