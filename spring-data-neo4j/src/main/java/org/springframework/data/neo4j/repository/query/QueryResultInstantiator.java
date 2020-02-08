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
package org.springframework.data.neo4j.repository.query;

import java.util.Map;
import java.util.Optional;

import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.reflect.ReflectionEntityInstantiator;
import org.neo4j.ogm.session.EntityInstantiator;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.conversion.Neo4jOgmEntityInstantiatorAdapter;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.lang.Nullable;

/**
 * This class uses the {@link Neo4jOgmEntityInstantiatorAdapter} that came with OGM 3.1.0+ if possible, otherwise falls back
 * to {@link org.neo4j.ogm.metadata.reflect.ReflectionEntityInstantiator}, appearing in the same release. Thus it
 * handles the conversion of non entity {@code QueryResult} objects the same way as entities, supporting non-default
 * constructors and such. The {@link Neo4jOgmEntityInstantiatorAdapter} cannot be used if Springs mapping context is not
 * available.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
class QueryResultInstantiator implements EntityInstantiator {

	private final EntityInstantiator delegate;

	QueryResultInstantiator(MetaData metadata,
			@Nullable MappingContext<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> mappingContext) {

		this.delegate = Optional.ofNullable(mappingContext)
				.<EntityInstantiator> map(ctx -> new Neo4jOgmEntityInstantiatorAdapter(ctx, null))
				.orElseGet(() -> new ReflectionEntityInstantiator(metadata));
	}

	@Override
	public <T> T createInstance(Class<T> clazz, Map<String, Object> propertyValues) {
		return this.delegate.createInstance(clazz, propertyValues);
	}
}
