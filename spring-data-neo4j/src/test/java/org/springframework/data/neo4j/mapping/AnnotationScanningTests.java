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
package org.springframework.data.neo4j.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.metadata.AnnotationsInfo;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Michael J. Simons
 * @soundtrack Metallica - S&M2
 */
@ContextConfiguration(classes = { AnnotationScanningTests.Config.class })
@RunWith(SpringRunner.class)
public class AnnotationScanningTests extends MultiDriverTestClass {

	@Autowired
	private SessionFactory sessionFactory;

	@Test
	public void shouldBeAbleToWorkWithAnnotatedAnnotations() {
		AnnotationsInfo annotations = sessionFactory.metaData().classInfo("TheEndpointController").annotationsInfo();
		assertThat(annotations.get("org.springframework.data.neo4j.mapping.datagraph1303.EndpointController"))
				.isNotNull().extracting(i -> i.get("value"))
				.isEqualTo("foobar");
	}

	@Configuration
	static class Config {

		@Bean
		public SessionFactory sessionFactory() {
			return new SessionFactory(getBaseConfiguration().build(),
					"org.springframework.data.neo4j.mapping.datagraph1303");
		}
	}
}
