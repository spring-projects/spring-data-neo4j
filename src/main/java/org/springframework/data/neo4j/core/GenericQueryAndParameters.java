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
package org.springframework.data.neo4j.core;

import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Functions;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.Relationship;
import org.neo4j.cypherdsl.core.Statement;
import org.springframework.data.neo4j.core.mapping.Constants;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

final class GenericQueryAndParameters {

	private final static String ROOT_NODE_IDS = "rootNodeIds";
	private final static String RELATIONSHIP_IDS = "relationshipIds";
	private final static String RELATED_NODE_IDS = "relatedNodeIds";

	final static Statement STATEMENT = createStatement();
	final static GenericQueryAndParameters EMPTY =
			new GenericQueryAndParameters(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());

	private final Map<String, Collection<Long>> parameters = new HashMap<>(3);

	GenericQueryAndParameters(Collection<Long> rootNodeIds, Collection<Long> relationshipsIds, Collection<Long> relatedNodeIds) {
		parameters.put(ROOT_NODE_IDS, rootNodeIds);
		parameters.put(RELATIONSHIP_IDS, relationshipsIds);
		parameters.put(RELATED_NODE_IDS, relatedNodeIds);
	}

	GenericQueryAndParameters(Collection<GenericQueryAndParameters> others) {

		parameters.put(ROOT_NODE_IDS, new HashSet<>());
		parameters.put(RELATIONSHIP_IDS, new HashSet<>());
		parameters.put(RELATED_NODE_IDS, new HashSet<>());
		for (GenericQueryAndParameters other : others) {
			parameters.get(ROOT_NODE_IDS).addAll((Collection<Long>) other.getParameters().get(ROOT_NODE_IDS));
			parameters.get(RELATIONSHIP_IDS).addAll((Collection<Long>) other.getParameters().get(RELATIONSHIP_IDS));
			parameters.get(RELATED_NODE_IDS).addAll((Collection<Long>) other.getParameters().get(RELATED_NODE_IDS));
		}

	}

	Map<String, Object> getParameters() {
		return Collections.unmodifiableMap(parameters);
	}

	boolean isEmpty() {
		return parameters.get(ROOT_NODE_IDS).isEmpty();
	}

	private static Statement createStatement() {
		Node rootNodes = Cypher.anyNode("rootNodes");
		Node relatedNodes = Cypher.anyNode("relatedNodes");
		Relationship relationships = Cypher.anyNode().relationshipBetween(Cypher.anyNode()).named("relationships");

		return Cypher.match(rootNodes)
				.where(Functions.id(rootNodes).in(Cypher.parameter(ROOT_NODE_IDS)))
				.optionalMatch(relationships)
					.where(Functions.id(relationships).in(Cypher.parameter(RELATIONSHIP_IDS)))
				.optionalMatch(relatedNodes)
					.where(Functions.id(relatedNodes).in(Cypher.parameter(RELATED_NODE_IDS)))
				.returning(
						rootNodes.as(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE),
						Functions.collectDistinct(relationships).as(Constants.NAME_OF_SYNTHESIZED_RELATIONS),
						Functions.collectDistinct(relatedNodes).as(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES)
				).build();
	}
}
