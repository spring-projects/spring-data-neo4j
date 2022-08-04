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
package org.springframework.data.neo4j.integration.issues.gh2576;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.test.BookmarkCapture;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class GH2576IT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Test
	@Tag("GH-2576")
	void listOfMapsShouldBeUsableAsArguments(@Autowired Neo4jTemplate template,
			@Autowired CollegeRepository collegeRepository) {

		Student student = template.save(new Student("S1"));
		College college = template.save(new College("C1"));

		Map<String, String> pair = new HashMap<>();
		pair.put("stuGuid", student.getGuid());
		pair.put("collegeGuid", college.getGuid());
		List<Map<String, String>> listOfPairs = Collections.singletonList(pair);

		List<String> uuids = collegeRepository.addStudentToCollege(listOfPairs);
		assertThat(uuids).containsExactly(student.getGuid());
	}

	@Test
	@Tag("GH-2576")
	void listOfMapsShouldBeUsableAsArgumentsWithWorkaround(@Autowired Neo4jTemplate template,
			@Autowired CollegeRepository collegeRepository) {

		Student student = template.save(new Student("S1"));
		College college = template.save(new College("C1"));

		Map<String, String> pair = new HashMap<>();
		pair.put("stuGuid", student.getGuid());
		pair.put("collegeGuid", college.getGuid());
		List<Value> listOfPairs = Collections.singletonList(Values.value(pair));

		List<String> uuids = collegeRepository.addStudentToCollegeWorkaround(listOfPairs);
		assertThat(uuids).containsExactly(student.getGuid());
	}

	@Configuration
	@EnableTransactionManagement
	@EnableNeo4jRepositories(considerNestedRepositories = true)
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public BookmarkCapture bookmarkCapture() {
			return new BookmarkCapture();
		}

		@Override
		public PlatformTransactionManager transactionManager(
				Driver driver, DatabaseSelectionProvider databaseNameProvider) {

			BookmarkCapture bookmarkCapture = bookmarkCapture();
			return new Neo4jTransactionManager(driver, databaseNameProvider,
					Neo4jBookmarkManager.create(bookmarkCapture));
		}

		@Override
		protected Collection<String> getMappingBasePackages() {
			return Collections.singleton(Student.class.getPackage().getName());
		}

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}
	}
}
