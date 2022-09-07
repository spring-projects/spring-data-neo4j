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
package org.springframework.data.neo4j.core.mapping;

import org.apiguardian.api.API;
import org.apiguardian.api.API.Status;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.SymbolicName;
import org.springframework.util.StringUtils;

import java.util.function.Function;

/**
 * A pool of constants used in our Cypher generation. These constants may change without further notice and are meant
 * for internal use only.
 *
 * @author Michael J. Simons
 * @soundtrack Milky Chance - Sadnecessary
 * @since 6.0
 */
@API(status = Status.EXPERIMENTAL, since = "6.0")
public final class Constants {

	public static final Function<NodeDescription<?>, SymbolicName> NAME_OF_TYPED_ROOT_NODE =
			(nodeDescription) -> nodeDescription != null
			? Cypher.name(StringUtils.uncapitalize(nodeDescription.getUnderlyingClass().getSimpleName()))
			: Cypher.name("n");

	public static final SymbolicName NAME_OF_ROOT_NODE = NAME_OF_TYPED_ROOT_NODE.apply(null);

	public static final String NAME_OF_INTERNAL_ID = "__internalNeo4jId__";
	/**
	 * Indicates the list of dynamic labels.
	 */
	public static final String NAME_OF_LABELS = "__nodeLabels__";
	/**
	 * Indicates the list of all labels.
	 */
	public static final String NAME_OF_ALL_LABELS = "__labels__";
	public static final String NAME_OF_IDS = "__ids__";
	public static final String NAME_OF_ID = "__id__";
	public static final String NAME_OF_VERSION_PARAM = "__version__";
	public static final String NAME_OF_PROPERTIES_PARAM = "__properties__";
	/**
	 * Indicates the parameter that contains the static labels which are required to correctly compute the difference
	 * in the list of dynamic labels when saving a node.
	 */
	public static final String NAME_OF_STATIC_LABELS_PARAM = "__staticLabels__";
	public static final String NAME_OF_ENTITY_LIST_PARAM = "__entities__";
	public static final String NAME_OF_RELATIONSHIP_LIST_PARAM = "__relationships__";
	public static final String NAME_OF_KNOWN_RELATIONSHIP_PARAM = "__knownRelationShipId__";
	public static final String NAME_OF_KNOWN_RELATIONSHIPS_PARAM = "__knownRelationShipIds__";
	public static final String NAME_OF_ALL_PROPERTIES = "__allProperties__";

	public static final String NAME_OF_SYNTHESIZED_ROOT_NODE = "__sn__";
	public static final String NAME_OF_SYNTHESIZED_RELATED_NODES = "__srn__";
	public static final String NAME_OF_SYNTHESIZED_RELATIONS = "__sr__";

	public static final String FROM_ID_PARAMETER_NAME = "fromId";
	public static final String TO_ID_PARAMETER_NAME = "toId";

	private Constants() {
	}
}
