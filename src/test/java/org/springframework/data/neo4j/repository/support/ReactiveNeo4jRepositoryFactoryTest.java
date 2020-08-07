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
package org.springframework.data.neo4j.repository.support;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.repository.core.RepositoryInformation;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class ReactiveNeo4jRepositoryFactoryTest {

	/**
	 * Test failure and success to ensure that {@link Neo4jRepositoryFactorySupport#assertIdentifierType(Class, Class)}
	 * gets used.
	 */
	@Nested
	class IdentifierTypeCheck {

		@Spy private ReactiveNeo4jRepositoryFactory neo4jRepositoryFactory = new ReactiveNeo4jRepositoryFactory(null, null);
		private Neo4jEntityInformation entityInformation;
		private RepositoryInformation metadata;

		@BeforeEach
		void setup() {

			metadata = mock(RepositoryInformation.class);
			entityInformation = mock(Neo4jEntityInformation.class);

			doReturn(entityInformation).when(neo4jRepositoryFactory).getEntityInformation(Mockito.any());
		}

		@Test
		void matchingClassTypes() {
			when(entityInformation.getIdType()).thenReturn(Long.class);
			Class repositoryIdentifierClass = Long.class;
			when(metadata.getIdType()).thenReturn(repositoryIdentifierClass);

			assertThatThrownBy(() -> neo4jRepositoryFactory.getTargetRepository(metadata))
					.hasMessage("Target type must not be null!").isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		void mismatchingClassTypes() {
			when(entityInformation.getIdType()).thenReturn(Long.class);
			Class repositoryIdentifierClass = String.class;
			when(metadata.getIdType()).thenReturn(repositoryIdentifierClass);

			assertThatThrownBy(() -> neo4jRepositoryFactory.getTargetRepository(metadata))
					.hasMessage(
							"The repository id type class java.lang.String differs from the entity id type class java.lang.Long.")
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

}
