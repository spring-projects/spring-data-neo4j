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
package org.springframework.data.neo4j.repository.config;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationUtils;

/**
 * @author Gerrit Meier
 */
class EnableNeo4jRepositoriesTests {

	private static final String BASE_PACKAGES_VALUE = "basePackages";

	@Test
	void valueIsAliasForBasePackages() {

		EnableNeo4jRepositories annotation = AnnotationUtils.findAnnotation(EnableRepositoryConfigWithValue.class,
				EnableNeo4jRepositories.class);

		assertThat(annotation).isNotNull();
		assertThat(annotation.value()).containsExactly(BASE_PACKAGES_VALUE);
		assertThat(annotation.value()).containsExactly(annotation.basePackages());
	}

	@Test
	void basePackagesIsAliasForValue() {

		EnableNeo4jRepositories annotation = AnnotationUtils.findAnnotation(EnableRepositoryConfigWithBasePackages.class,
				EnableNeo4jRepositories.class);

		assertThat(annotation).isNotNull();
		assertThat(annotation.basePackages()).containsExactly(BASE_PACKAGES_VALUE);
		assertThat(annotation.basePackages()).containsExactly(annotation.value());
	}

	@EnableNeo4jRepositories(BASE_PACKAGES_VALUE)
	private class EnableRepositoryConfigWithValue {
	}

	@EnableNeo4jRepositories(basePackages = BASE_PACKAGES_VALUE)
	private class EnableRepositoryConfigWithBasePackages {
	}

}
