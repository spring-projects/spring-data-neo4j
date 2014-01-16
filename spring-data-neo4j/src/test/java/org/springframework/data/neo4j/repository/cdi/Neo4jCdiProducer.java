/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository.cdi;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import org.springframework.data.neo4j.core.GraphDatabase;
import org.springframework.data.neo4j.support.GraphDatabaseFactoryBean;

/**
 * Simple component exposing a {@link GraphDatabase} as CDI bean.
 * 
 * @author Nicki Watt
 * @author Oliver Gierke
 */
class Neo4jCdiProducer {

	@Produces
	@ApplicationScoped
	GraphDatabase createGraphDatabase() throws Exception {

		GraphDatabaseFactoryBean factory = new GraphDatabaseFactoryBean();
		factory.setStoreLocation("target/cdi-test-db");

		return factory.getObject();
	}
	
	void shutdown(@Disposes GraphDatabase database) {
	  database.shutdown();
	}
}
