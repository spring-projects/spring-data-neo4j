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
package org.springframework.data.neo4j.config;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.inject.Qualifier;

import org.apiguardian.api.API;

/**
 * An internally used CDI {@link Qualifier} to mark all beans produced by our
 * {@link Neo4jCdiConfigurationSupport configuration support} as built in.
 * When the {@link Neo4jCdiExtension Spring Data Neo4j CDI extension} is used,
 * you can opt in to override any of the following beans by providing a {@link jakarta.enterprise.inject.Produces @Produces} method with the
 * corresponding return type:
 * <ul>
 *     <li>{@link org.springframework.data.neo4j.core.convert.Neo4jConversions}</li>
 *     <li>{@link org.springframework.data.neo4j.core.DatabaseSelectionProvider}</li>
 *     <li>{@link org.springframework.data.neo4j.core.Neo4jOperations}</li>
 * </ul>
 * The order in which the types are presented reflects the usefulness over overriding such a bean.
 * You might want to add additional conversions to the mapping or provide a bean that dynamically selects a Neo4j database.
 * Running a custom bean of the template or client might prove useful if you want to add additional methods.
 *
 * @author Michael J. Simons
 * @soundtrack Buckethead - SIGIL Soundtrack
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
public @interface Builtin {
}
