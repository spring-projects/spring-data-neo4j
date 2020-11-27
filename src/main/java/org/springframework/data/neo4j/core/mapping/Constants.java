/*
 * Copyright 2011-2020 the original author or authors.
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

/**
 * A pool of constants used in our Cypher generation. These constants may change without further notice and are meant
 * for internal use only.
 *
 * @author Michael J. Simons
 * @soundtrack Milky Chance - Sadnecessary
 * @since 6.0
 */
@API(status = Status.INTERNAL, since = "6.0")
public final class Constants {

	public static final SymbolicName NAME_OF_ROOT_NODE = Cypher.name("n");

	public static final String NAME_OF_INTERNAL_ID = "__internalNeo4jId__";
	public static final String NAME_OF_LABELS = "__nodeLabels__";
	public static final String NAME_OF_IDS = "__ids__";
	public static final String NAME_OF_ID = "__id__";
	public static final String NAME_OF_VERSION_PARAM = "__version__";
	public static final String NAME_OF_PROPERTIES_PARAM = "__properties__";
	public static final String NAME_OF_STATIC_LABELS_PARAM = "__staticLabels__";
	public static final String NAME_OF_ENTITY_LIST_PARAM = "__entities__";
	public static final String NAME_OF_PATHS = "__paths__";
	public static final String NAME_OF_ALL_PROPERTIES = "__allProperties__";
	public static final String NAME_OF_IS_PATH_SEGMENT = "__is_path_segment__";

	public static final String PATH_START = "__start__";
	public static final String PATH_RELATIONSHIP = "__relationship__";
	public static final String PATH_END = "__end__";

	public static final String FROM_ID_PARAMETER_NAME = "fromId";

	private Constants() {
	}
}
