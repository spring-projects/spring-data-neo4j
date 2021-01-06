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
package org.springframework.data.neo4j.repository.support;

import org.apiguardian.api.API;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;

/**
 * A PersistenceExceptionTranslator to get picked up by the Spring exception translation infrastructure.
 *
 * @author Michael J. Simons
 * @soundtrack Kummer - KIOX
 * @since 6.0
 * @deprecated since 6.0.3 Use {@link org.springframework.data.neo4j.core.Neo4jPersistenceExceptionTranslator} instead.
 */
@Deprecated
@API(status = API.Status.DEPRECATED, since = "6.0")
public final class Neo4jPersistenceExceptionTranslator implements PersistenceExceptionTranslator {

	private final org.springframework.data.neo4j.core.Neo4jPersistenceExceptionTranslator delegate =
			new org.springframework.data.neo4j.core.Neo4jPersistenceExceptionTranslator();

	@Override
	public DataAccessException translateExceptionIfPossible(RuntimeException ex) {
		return delegate.translateExceptionIfPossible(ex);
	}
}
