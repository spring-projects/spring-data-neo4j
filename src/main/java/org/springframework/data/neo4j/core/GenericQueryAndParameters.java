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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

final class GenericQueryAndParameters {

	private final static String ROOT_NODE_IDS = "rootNodeIds";
	private final static String RELATIONSHIP_IDS = "relationshipIds";
	private final static String RELATED_NODE_IDS = "relatedNodeIds";

	final static GenericQueryAndParameters EMPTY =
			new GenericQueryAndParameters(Collections.emptySet(), Collections.emptySet(), Collections.emptySet());

	private final Map<String, Collection<Long>> parameters = new HashMap<>(3);

	GenericQueryAndParameters(Collection<Long> rootNodeIds, Collection<Long> relationshipsIds, Collection<Long> relatedNodeIds) {
		parameters.put(ROOT_NODE_IDS, rootNodeIds);
		parameters.put(RELATIONSHIP_IDS, relationshipsIds);
		parameters.put(RELATED_NODE_IDS, relatedNodeIds);
	}

	GenericQueryAndParameters() {
		this(new HashSet<>(), new HashSet<>(), new HashSet<>());
	}

	void with(Collection<Long> rootNodeIds, Collection<Long> relationshipsIds, Collection<Long> relatedNodeIds) {
		parameters.put(ROOT_NODE_IDS, rootNodeIds);
		parameters.put(RELATIONSHIP_IDS, relationshipsIds);
		parameters.put(RELATED_NODE_IDS, relatedNodeIds);
	}

	Map<String, Object> getParameters() {
		return Collections.unmodifiableMap(parameters);
	}

	boolean isEmpty() {
		return parameters.get(ROOT_NODE_IDS).isEmpty();
	}

}
