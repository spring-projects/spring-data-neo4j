/*
 * Copyright 2011-2022 the original author or authors.
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

/**
 * Shared repository factory functionality between imperative and reactive world.
 *
 * @author Gerrit Meier
 */
final class Neo4jRepositoryFactorySupport {

	static void assertIdentifierType(Class<?> repositoryIdType, Class<?> entityIdType) {

		if (repositoryIdType.equals(entityIdType) || isCompatibleType(repositoryIdType, entityIdType)) {
			return;
		}

		String errorMessage = String.format("The repository id type %s differs from the entity id type %s",
				repositoryIdType, entityIdType);

		throw new IllegalArgumentException(errorMessage);
	}

	private static boolean isCompatibleType(Class<?> repositoryIdType, Class<?> entityIdType) {
		return isCompatibleLongType(repositoryIdType, entityIdType)
				|| isCompatibleIntegerType(repositoryIdType, entityIdType);
	}

	private static boolean isCompatibleLongType(Class<?> repositoryIdType, Class<?> entityIdType) {
		return repositoryIdType.equals(Long.class) && entityIdType.equals(long.class)
				|| repositoryIdType.equals(long.class) && entityIdType.equals(Long.class);
	}

	private static boolean isCompatibleIntegerType(Class<?> repositoryIdType, Class<?> entityIdType) {
		return repositoryIdType.equals(Integer.class) && entityIdType.equals(int.class)
				|| repositoryIdType.equals(int.class) && entityIdType.equals(Integer.class);
	}

	private Neo4jRepositoryFactorySupport() {
	}
}
