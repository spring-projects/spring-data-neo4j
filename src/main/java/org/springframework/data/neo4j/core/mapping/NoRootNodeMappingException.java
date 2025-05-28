/*
 * Copyright 2011-2025 the original author or authors.
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

import java.io.Serial;
import java.util.Formattable;
import java.util.Formatter;
import java.util.Locale;

import org.apiguardian.api.API;
import org.jspecify.annotations.Nullable;
import org.neo4j.driver.types.MapAccessor;
import org.springframework.data.mapping.MappingException;

/**
 * A {@link NoRootNodeMappingException} is thrown when the entity converter cannot find a node or map like structure
 * that can be mapped.
 * Nodes eligible for mapping are actual nodes with at least the primary label attached or exactly one map structure
 * that is neither a node nor relationship itself.
 *
 * @author Michael J. Simons
 * @soundtrack Helge Schneider - Sammlung Schneider! Musik und Lifeshows!
 * @since 6.0.2
 */
@API(status = API.Status.INTERNAL, since = "6.0.2")
public final class NoRootNodeMappingException extends MappingException implements Formattable {

	@Serial
	private static final long serialVersionUID = 5742846435191601546L;

	@Nullable
	private final transient MapAccessor mapAccessor;
	@Nullable
	private final transient Neo4jPersistentEntity<?> entity;

	@SuppressWarnings("NullableProblems")
	public NoRootNodeMappingException(MapAccessor mapAccessor, Neo4jPersistentEntity<?> entity) {
		super(String.format("Could not find mappable nodes or relationships inside %s for %s", mapAccessor, entity));
		this.mapAccessor = mapAccessor;
		this.entity = entity;
	}

	@Override
	public void formatTo(Formatter formatter, int flags, int width, int precision) {
		if (mapAccessor != null && entity != null) {
			String className = entity.getUnderlyingClass().getSimpleName();
			formatter.format("Could not find mappable nodes or relationships inside %s for %s:%s", mapAccessor,
					className.substring(0, 1).toLowerCase(
							Locale.ROOT), String.join(":", entity.getStaticLabels()));
		} else {
			formatter.format("Could not find mappable nodes or relationships inside a record");
		}
	}
}
