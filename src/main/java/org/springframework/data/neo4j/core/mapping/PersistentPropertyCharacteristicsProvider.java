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
package org.springframework.data.neo4j.core.mapping;

import static org.apiguardian.api.API.Status.STABLE;

import java.util.function.BiFunction;

import org.apiguardian.api.API;
import org.springframework.data.mapping.model.Property;

/**
 * An instance of such a provider can be registered as a Spring bean and will be consulted by the {@link Neo4jMappingContext}
 * prior to creating and populating {@link Neo4jPersistentProperty persistent properties}.
 *
 * @author Michael J. Simons
 * @soundtrack Metallica - Kill 'Em All
 * @since 6.3.7
 */
@API(status = STABLE, since = "6.3.7")
public interface PersistentPropertyCharacteristicsProvider extends BiFunction<Property, Neo4jPersistentEntity<?>, PersistentPropertyCharacteristics> {
}
