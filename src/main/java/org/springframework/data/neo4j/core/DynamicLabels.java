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
package org.springframework.data.neo4j.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.UnaryOperator;

import org.jspecify.annotations.Nullable;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.cypherdsl.core.StatementBuilder.OngoingMatchAndUpdate;

import org.springframework.data.neo4j.core.mapping.Constants;
import org.springframework.data.neo4j.core.mapping.NodeDescription;

/**
 * Decorator for an ongoing update statement that removes obsolete dynamic labels and adds
 * new ones.
 *
 * @author Michael J. Simons
 */
final class DynamicLabels implements UnaryOperator<OngoingMatchAndUpdate> {

	public static final DynamicLabels EMPTY = new DynamicLabels(null, Collections.emptyList(), Collections.emptyList());

	private final Node rootNode;

	private final List<String> oldLabels;

	private final List<String> newLabels;

	DynamicLabels(@Nullable NodeDescription<?> nodeDescription, Collection<String> oldLabels,
			@Nullable Collection<String> newLabels) {
		this.oldLabels = new ArrayList<>(oldLabels);
		this.newLabels = (newLabels != null) ? new ArrayList<>(newLabels) : List.of();
		this.rootNode = Cypher.anyNode(Constants.NAME_OF_TYPED_ROOT_NODE.apply(nodeDescription));
	}

	@Override
	public OngoingMatchAndUpdate apply(OngoingMatchAndUpdate ongoingMatchAndUpdate) {

		OngoingMatchAndUpdate decoratedMatchAndUpdate = ongoingMatchAndUpdate;
		if (!this.oldLabels.isEmpty()) {
			decoratedMatchAndUpdate = decoratedMatchAndUpdate.remove(this.rootNode,
					this.oldLabels.toArray(new String[0]));
		}
		if (!this.newLabels.isEmpty()) {
			decoratedMatchAndUpdate = decoratedMatchAndUpdate.set(this.rootNode, this.newLabels.toArray(new String[0]));
		}
		return decoratedMatchAndUpdate;
	}

}
