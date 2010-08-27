/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.datastore.graph.neo4j.support;

import java.util.List;

import org.springframework.data.core.DataMapper;
import org.springframework.data.core.QueryDefinition;
import org.springframework.datastore.core.AbstractDatastoreTemplate;

import org.neo4j.graphdb.GraphDatabaseService;

public class Neo4jDatastoreTemplate extends AbstractDatastoreTemplate<GraphDatabaseService> {

	
	public Neo4jDatastoreTemplate(Neo4jConnectionFactory connectionFactory) {
		super();
		setDatastoreConnectionFactory(connectionFactory);
	}

	public Neo4jDatastoreTemplate(GraphDatabaseService graphDatabaseService) {
		super();
		setDatastoreConnectionFactory(new Neo4jConnectionFactory(graphDatabaseService));
	}

	@Override
	public <S, T> List<T> query(QueryDefinition arg0, DataMapper<S, T> arg1) {
		return null;
	}

	
	
}
