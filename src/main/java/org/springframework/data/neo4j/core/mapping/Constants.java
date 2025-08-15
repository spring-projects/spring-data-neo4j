/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.core.mapping;

import java.util.function.Function;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.SymbolicName;

import org.springframework.util.StringUtils;

/**
 * A pool of constants used in our Cypher generation. These constants may change without
 * further notice and are meant for internal use only.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@API(status = Status.EXPERIMENTAL, since = "6.0")
public final class Constants {

	/**
	 * A function for deriving a name for the root node of a query.
	 */
	public static final Function<NodeDescription<?>, SymbolicName> NAME_OF_TYPED_ROOT_NODE = (
			nodeDescription) -> (nodeDescription != null)
					? Cypher.name(StringUtils.uncapitalize(nodeDescription.getUnderlyingClass().getSimpleName()))
					: Cypher.name("n");

	/**
	 * A generic name for an untyped root node.
	 */
	public static final SymbolicName NAME_OF_ROOT_NODE = NAME_OF_TYPED_ROOT_NODE.apply(null);

	/**
	 * The name of the property SDN uses to transport the internal Neo4j entity id.
	 */
	public static final String NAME_OF_INTERNAL_ID = "__internalNeo4jId__";

	/**
	 * The name of the property SDN uses to transport the Neo4j element id.
	 */
	public static final String NAME_OF_ELEMENT_ID = "__elementId__";

	/**
	 * The name of a property SDN might insert to guarantee a stable sort of records.
	 */
	public static final String NAME_OF_ADDITIONAL_SORT = "__stable_uniq_sort__";

	/**
	 * Indicates the list of dynamic labels.
	 */
	public static final String NAME_OF_LABELS = "__nodeLabels__";

	/**
	 * Indicates the list of all labels.
	 */
	public static final String NAME_OF_ALL_LABELS = "__labels__";

	/**
	 * The name of the property SDN uses to transport a set of ids.
	 */
	public static final String NAME_OF_IDS = "__ids__";

	/**
	 * The name of the property SDN uses to transport an id.
	 */
	public static final String NAME_OF_ID = "__id__";

	/**
	 * The name of the property SDN uses to transport the version of an entity.
	 */
	public static final String NAME_OF_VERSION_PARAM = "__version__";

	/**
	 * The name of the property SDN uses to transport all projected properties.
	 */
	public static final String NAME_OF_PROPERTIES_PARAM = "__properties__";

	/**
	 * The name of the property SDN uses to transport a vector property.
	 */
	public static final String NAME_OF_VECTOR_PROPERTY = "__vectorProperty__";

	/**
	 * The name of the property SDN uses to transport the value of a vector property.
	 */
	public static final String NAME_OF_VECTOR_VALUE = "__vectorValue__";

	/**
	 * Indicates the parameter that contains the static labels which are required to
	 * correctly compute the difference in the list of dynamic labels when saving a node.
	 */
	public static final String NAME_OF_STATIC_LABELS_PARAM = "__staticLabels__";

	/**
	 * The name of the parameter SDN uses to pass a list of entities.
	 */
	public static final String NAME_OF_ENTITY_LIST_PARAM = "__entities__";

	/**
	 * The name of the parameter SDN uses to pass a list of relationships.
	 */
	public static final String NAME_OF_RELATIONSHIP_LIST_PARAM = "__relationships__";

	/**
	 * The name of the parameter SDN uses to pass a known relationship id.
	 */
	public static final String NAME_OF_KNOWN_RELATIONSHIP_PARAM = "__knownRelationShipId__";

	/**
	 * The name of the parameter SDN uses to pass a list of known relationship ids.
	 */
	public static final String NAME_OF_KNOWN_RELATIONSHIPS_PARAM = "__knownRelationShipIds__";

	/**
	 * The name of the parameter SDN uses to pass all properties.
	 */
	public static final String NAME_OF_ALL_PROPERTIES = "__allProperties__";

	/**
	 * Optional property for relationship properties' simple class name to keep type info.
	 */
	public static final String NAME_OF_RELATIONSHIP_TYPE = "__relationshipType__";

	/**
	 * The name SDN uses for a synthesized root node.
	 */
	public static final String NAME_OF_SYNTHESIZED_ROOT_NODE = "__sn__";

	/**
	 * The name SDN uses for synthesized related nodes.
	 */
	public static final String NAME_OF_SYNTHESIZED_RELATED_NODES = "__srn__";

	/**
	 * The name SDN uses for synthesized relationships.
	 */
	public static final String NAME_OF_SYNTHESIZED_RELATIONS = "__sr__";

	/**
	 * The name SDN uses for the parameter to pass the "from id".
	 */
	public static final String FROM_ID_PARAMETER_NAME = "fromId";

	/**
	 * The name SDN uses for the parameter to pass the "to id".
	 */
	public static final String TO_ID_PARAMETER_NAME = "toId";

	/**
	 * The name SDN uses for vector search score.
	 */
	public static final String NAME_OF_SCORE = "__score__";

	/**
	 * Vector search vector parameter name.
	 */
	public static final String VECTOR_SEARCH_VECTOR_PARAMETER = "vectorSearchParam";

	/**
	 * Vector search score parameter name.
	 */
	public static final String VECTOR_SEARCH_SCORE_PARAMETER = "scoreParam";

	private Constants() {
	}

}
