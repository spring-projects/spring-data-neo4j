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

import java.util.Collection;

/**
 * Wraps a resolved node description together with the complete list of labels returned
 * from the database and the list of labels not statically defined in the resolved node
 * hierarchy.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
final class NodeDescriptionAndLabels {

	private final NodeDescription<?> nodeDescription;

	private final Collection<String> dynamicLabels;

	NodeDescriptionAndLabels(NodeDescription<?> nodeDescription, Collection<String> dynamicLabels) {
		this.nodeDescription = nodeDescription;
		this.dynamicLabels = dynamicLabels;
	}

	NodeDescription<?> getNodeDescription() {
		return this.nodeDescription;
	}

	Collection<String> getDynamicLabels() {
		return this.dynamicLabels;
	}

}
