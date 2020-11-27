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
package org.springframework.data.neo4j.core.mapping;

import org.apiguardian.api.API;
import org.springframework.data.mapping.MappingException;

/**
 * A {@link NoRootNodeMappingException} is thrown when the entity converter cannot find a node or map like structure
 * that can be mapped.
 * Nodes eligible for mapping are actual nodes with at least the primarly label attached or exactly one map structure
 * that is neither a node or relationship itself.
 *
 * @author Michael J. Simons
 * @soundtrack Helge Schneider - Sammlung Schneider! Musik und Lifeshows!
 * @since 6.0.2
 */
@API(status = API.Status.INTERNAL, since = "6.0.2")
public final class NoRootNodeMappingException extends MappingException {

	public NoRootNodeMappingException(String s) {
		super(s);
	}
}
