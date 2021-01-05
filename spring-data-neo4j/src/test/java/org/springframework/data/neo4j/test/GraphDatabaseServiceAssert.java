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
package org.springframework.data.neo4j.test;

import java.util.Collections;
import java.util.Map;

import org.assertj.core.api.AbstractAssert;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

/**
 * @author Michael J. Simons
 */
public class GraphDatabaseServiceAssert extends AbstractAssert<GraphDatabaseServiceAssert, GraphDatabaseService> {

	GraphDatabaseServiceAssert(GraphDatabaseService graphDatabaseService) {
		super(graphDatabaseService, GraphDatabaseServiceAssert.class);
	}

	public static GraphDatabaseServiceAssert assertThat(GraphDatabaseService actual) {
		return new GraphDatabaseServiceAssert(actual);
	}

	public NodeAssert containsNode(String matchStatement) {
		return containsNode(matchStatement, Collections.emptyMap());
	}

	public NodeAssert containsNode(String matchStatement, Map<String, Object> params) {

		isNotNull();

		NodeAssert nodeAssert;
		try (Transaction tx = actual.beginTx(); Result result = actual.execute(matchStatement, params)) {

			if (!result.hasNext()) {
				tx.failure();
				failWithMessage("Graph should contain node matched by <%s> didn't.", matchStatement);
			}

			tx.success();
			nodeAssert = new NodeAssert((Node) result.next().get("n"));
		}
		return nodeAssert;
	}
}
