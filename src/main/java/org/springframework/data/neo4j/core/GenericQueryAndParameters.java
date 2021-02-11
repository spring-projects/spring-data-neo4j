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
import java.util.Set;

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
		Node chef = Cypher.anyNode("rootNodes");
		Node rest = Cypher.anyNode("relatedNodes");
		Relationship relationship = Cypher.anyNode().relationshipBetween(Cypher.anyNode()).named("relationships");

		return Cypher.match(chef)
				.where(Functions.id(chef).in(Cypher.parameter(ROOT_NODE_IDS)))
				.optionalMatch(relationship)
					.where(Functions.id(relationship).in(Cypher.parameter(RELATIONSHIP_IDS)))
				.optionalMatch(rest)
					.where(Functions.id(rest).in(Cypher.parameter(RELATED_NODE_IDS)))
				.returning(
						chef.as(Constants.NAME_OF_SYNTHESIZED_ROOT_NODE),
						Functions.collectDistinct(relationship).as(Constants.NAME_OF_SYNTHESIZED_RELATIONS),
						Functions.collectDistinct(rest).as(Constants.NAME_OF_SYNTHESIZED_RELATED_NODES)
				).build();
	}
}
