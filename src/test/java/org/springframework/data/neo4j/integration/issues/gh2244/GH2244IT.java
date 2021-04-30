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
package org.springframework.data.neo4j.integration.issues.gh2244;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.AbstractNeo4jConfig;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.test.Neo4jExtension;
import org.springframework.data.neo4j.test.Neo4jIntegrationTest;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class GH2244IT {

	protected static Neo4jExtension.Neo4jConnectionSupport neo4jConnectionSupport;

	@Test
	void safeAllWithSubTypesShouldWork(@Autowired Neo4jTemplate neo4jTemplate) {

		List<Step> steps = Arrays.asList(new Step.Origin(), new Step.Chain(), new Step.End());
		steps = neo4jTemplate.saveAll(steps);
		assertThat(steps).allSatisfy(s -> assertThat(s.id).isNotNull());
	}

	/**
	 * Abstract domain class.
	 */
	@Node
	public static abstract class Step {

		@Id @GeneratedValue
		private Long id;

		/**
		 * A step.
		 */
		@Node
		public static class Chain extends Step {
		}

		/**
		 * A step.
		 */
		@Node
		public static class End extends Step {
		}

		/**
		 * A step.
		 */
		@Node
		public static class Origin extends Step {
		}
	}

	@Configuration
	@EnableTransactionManagement
	static class Config extends AbstractNeo4jConfig {

		@Bean
		public Driver driver() {

			return neo4jConnectionSupport.getDriver();
		}

		@Override
		public Neo4jMappingContext neo4jMappingContext(Neo4jConversions neo4JConversions)
				throws ClassNotFoundException {

			Neo4jMappingContext ctx = new Neo4jMappingContext(neo4JConversions);
			ctx.setInitialEntitySet(new HashSet<>(Arrays.asList(Step.class, Step.Chain.class, Step.End.class,
					Step.Origin.class)));
			return ctx;
		}
	}
}
