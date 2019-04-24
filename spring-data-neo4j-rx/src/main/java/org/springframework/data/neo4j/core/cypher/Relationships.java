/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.cypher;

import static org.springframework.data.neo4j.core.cypher.support.Visitable.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.neo4j.core.cypher.support.Visitor;

/**
 * Represents a chain of relationships. The chain is meant to be in order and the right node of an element is related to
 * the left node of the next element.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
public class Relationships implements PatternElement {

	private List<Relationship> relationships;

	Relationships(List<Relationship> relationships) {
		this.relationships = new ArrayList(relationships);
	}

	@Override
	public void accept(Visitor visitor) {

		visitor.enter(this);

		Node lastNode = null;
		for (Relationship relationship : relationships) {

			relationship.getLeft().accept(visitor);
			relationship.getDetails().accept(visitor);

			lastNode = relationship.getRight();
		}

		visitIfNotNull(lastNode, visitor);

		visitor.leave(this);
	}
}
