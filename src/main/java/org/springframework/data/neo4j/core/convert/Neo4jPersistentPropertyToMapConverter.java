/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.core.convert;

import java.util.Map;

import org.apiguardian.api.API;
import org.neo4j.driver.Value;
import org.springframework.lang.Nullable;

/**
 * You need to provide an implementation of this interface in case you want to store a property of an entity as separate
 * properties on a node. The entity needs to be decomposed into a map and composed from a map for that purpose.
 *
 * <p>The calling mechanism will take care of adding and removing configured prefixes and transforming keys and values into
 * something that Neo4j can understand.
 *
 * @author Michael J. Simons
 * @param <K> The type of the keys (Only Strings and Enums are supported).
 * @param <P> The type of the property.
 * @soundtrack Metallica - Helping Hands… Live & Acoustic At The Masonic
 * @since 6.0
 */
@API(status = API.Status.STABLE, since = "6.0")
public interface Neo4jPersistentPropertyToMapConverter<K, P> {

	/**
	 * Decomposes an object into a map. A conversion service is provided in case delegation is needed.
	 *
	 * @param property               The source property
	 * @param neo4jConversionService The conversion service to delegate to if necessary
	 * @return The decomposed object.
	 */
	Map<K, Value> decompose(@Nullable P property, Neo4jConversionService neo4jConversionService);

	/**
	 * Composes the object back from the map. The map contains the raw driver values, as SDN cannot know how you want to
	 * handle them. Therefore, the conversion service to convert driver values is provided.
	 *
	 * @param source                 The source map
	 * @param neo4jConversionService The conversion service in case you want to delegate the work for some values in the map
	 * @return The composed object.
	 */
	P compose(Map<K, Value> source, Neo4jConversionService neo4jConversionService);
}
