/*
 * Copyright (c) 2023-2025 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.springframework.data.falkordb.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.falkordb.core.DefaultFalkorDBClient;
import org.springframework.data.falkordb.core.FalkorDBClient;
import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBEntityConverter;
import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBMappingContext;
import org.springframework.data.falkordb.core.mapping.FalkorDBMappingContext;
import org.springframework.data.falkordb.repository.config.EnableFalkorDBRepositories;
import org.springframework.data.mapping.model.EntityInstantiators;

import com.falkordb.Driver;
import com.falkordb.impl.api.DriverImpl;

/**
 * Integration test to verify that @Query annotation support works correctly. This test
 * specifically validates that the bug "You have defined query methods in the repository
 * but do not have any query lookup strategy defined" is resolved.
 *
 * @author Shahar Biron
 * @since 1.0
 */
class QueryAnnotationSupportTest {

	private AnnotationConfigApplicationContext context;

	private TwitterUserRepository repository;

	@BeforeEach
	void setUp() {
		// Create Spring context with our configuration
		// If the bug exists, this will fail with "query lookup strategy not defined"
		context = new AnnotationConfigApplicationContext(TestConfig.class);
		repository = context.getBean(TwitterUserRepository.class);
	}

	@AfterEach
	void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	/**
	 * This test verifies that the Spring application context starts successfully with a
	 * repository that has @Query annotated methods. The bug would cause the application
	 * to fail during startup with: "You have defined query methods in the repository but
	 * do not have any query lookup strategy defined."
	 */
	@Test
	void shouldLoadRepositoryWithQueryAnnotations() {
		// If we reach this point, it means the repository was successfully instantiated
		// This proves the QueryLookupStrategy is properly configured
		assertThat(repository).as("Repository should be autowired successfully").isNotNull();
	}

	/**
	 * This test verifies that the repository methods are accessible and have proper
	 * metadata.
	 */
	@Test
	void shouldHaveQueryAnnotatedMethods() throws NoSuchMethodException {
		assertThat(repository).isNotNull();

		// Verify @Query annotated methods exist
		assertThat(TwitterUserRepository.class.getMethod("findFollowing", String.class)).isNotNull();
		assertThat(TwitterUserRepository.class.getMethod("findFollowers", String.class)).isNotNull();
		assertThat(TwitterUserRepository.class.getMethod("findTopVerifiedUsers", Integer.class, Boolean.class))
			.isNotNull();
		assertThat(TwitterUserRepository.class.getMethod("countFollowing", String.class)).isNotNull();
		assertThat(TwitterUserRepository.class.getMethod("countFollowers", String.class)).isNotNull();

		// Verify derived query methods exist
		assertThat(TwitterUserRepository.class.getMethod("findByUsername", String.class)).isNotNull();
		assertThat(TwitterUserRepository.class.getMethod("findByDisplayNameContaining", String.class)).isNotNull();
	}

	/**
	 * Configuration class for test context.
	 */
	@Configuration
	@EnableFalkorDBRepositories(basePackageClasses = TwitterUserRepository.class)
	static class TestConfig {

		@Bean
		public Driver falkorDBDriver() {
			// Use a mock or test instance
			return new DriverImpl("localhost", 6379);
		}

		@Bean
		public FalkorDBClient falkorDBClient(Driver driver) {
			return new DefaultFalkorDBClient(driver, "test_query_annotation");
		}

		@Bean
		public FalkorDBMappingContext falkorDBMappingContext() {
			return new DefaultFalkorDBMappingContext();
		}

		@Bean
		public FalkorDBTemplate falkorDBTemplate(FalkorDBClient client, FalkorDBMappingContext mappingContext) {
			EntityInstantiators instantiators = new EntityInstantiators();
			DefaultFalkorDBEntityConverter converter = new DefaultFalkorDBEntityConverter(mappingContext, instantiators,
					client);
			return new FalkorDBTemplate(client, mappingContext, converter);
		}

	}

}
