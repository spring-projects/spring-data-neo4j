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
package org.springframework.data.neo4j.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Michael J. Simons
 */
@ExtendWith(MockitoExtension.class)
class Neo4jAuditingRegistrarTests {

	@Mock
	AnnotationMetadata metadata;

	@Mock
	BeanDefinitionRegistry registry;

	Neo4jAuditingRegistrar registrar = new Neo4jAuditingRegistrar();

	@Test
	void rejectsNullAnnotationMetadata() {

		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.registrar.registerBeanDefinitions(null, this.registry));
	}

	@Test
	void rejectsNullBeanDefinitionRegistry() {

		assertThatIllegalArgumentException()
			.isThrownBy(() -> this.registrar.registerBeanDefinitions(this.metadata, null));
	}

}
