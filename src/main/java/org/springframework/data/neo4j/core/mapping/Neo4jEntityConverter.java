/*
 * Copyright 2011-2021 the original author or authors.
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

import java.util.Map;

import org.apiguardian.api.API;
import org.neo4j.driver.types.MapAccessor;
import org.springframework.data.convert.EntityReader;
import org.springframework.data.convert.EntityWriter;

/**
 * This orchestrates the built-in store conversions and any additional Spring converters.
 *
 * @author Michael J. Simons
 * @soundtrack The Kleptones - A Night At The Hip-Hopera
 * @since 6.0
 */
@API(status = API.Status.INTERNAL, since = "6.0")
public interface Neo4jEntityConverter extends EntityReader<Object, MapAccessor>, EntityWriter<Object, Map<String, Object>> {
}
