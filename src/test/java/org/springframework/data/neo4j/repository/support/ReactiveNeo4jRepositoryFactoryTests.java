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
package org.springframework.data.neo4j.repository.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class ReactiveNeo4jRepositoryFactoryTests {

	/**
	 * Test failure and success to ensure that
	 * {@link Neo4jRepositoryFactorySupport#assertIdentifierType(Class, Class)} gets used.
	 */
	@Nested
	class IdentifierTypeCheck {

		@Spy
		private ReactiveNeo4jRepositoryFactory neo4jRepositoryFactory = new ReactiveNeo4jRepositoryFactory(null, null);

		private Neo4jEntityInformation entityInformation;

		private RepositoryInformation metadata;

		@BeforeEach
		void setup() {

			this.metadata = mock(RepositoryInformation.class);
			this.entityInformation = mock(Neo4jEntityInformation.class);

			doReturn(this.entityInformation).when(this.neo4jRepositoryFactory)
				.getEntityInformation(Mockito.any(RepositoryMetadata.class));
		}

		@Test
		void matchingClassTypes() {
			given(this.entityInformation.getIdType()).willReturn(Long.class);
			Class repositoryIdentifierClass = Long.class;
			given(this.metadata.getIdType()).willReturn(repositoryIdentifierClass);

			assertThatThrownBy(() -> this.neo4jRepositoryFactory.getTargetRepository(this.metadata))
				.hasMessageContaining("Target type must not be null")
				.isInstanceOf(IllegalArgumentException.class);
		}

		@Test
		void mismatchingClassTypes() {
			given(this.entityInformation.getIdType()).willReturn(Long.class);
			Class repositoryIdentifierClass = String.class;
			given(this.metadata.getIdType()).willReturn(repositoryIdentifierClass);

			assertThatThrownBy(() -> this.neo4jRepositoryFactory.getTargetRepository(this.metadata)).hasMessage(
					"The repository id type class java.lang.String differs from the entity id type class java.lang.Long")
				.isInstanceOf(IllegalArgumentException.class);
		}

	}

}
