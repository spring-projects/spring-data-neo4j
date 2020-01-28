/*
 * Copyright (c) 2019-2020 "Neo4j,"
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
package org.neo4j.springframework.data.core.cypher.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class helps to group items of the same type on the same level of the tree into a list structure that can be
 * recognized by visitors.
 *
 * @author Michael J. Simons
 * @param <T> The children's type
 * @param <SELF> The concrete type of the implementing class.
 * @since 1.0
 */
public abstract class TypedSubtree<T extends Visitable, SELF extends TypedSubtree<T, SELF>> implements Visitable {

	protected final List<T> children;

	protected TypedSubtree(T... children) {

		this.children = Arrays.asList(children);
	}

	protected TypedSubtree(List<T> children) {

		this.children = new ArrayList<>(children);
	}

	@Override
	public final void accept(Visitor visitor) {

		visitor.enter(this);
		this.children.forEach(child -> prepareVisit(child).accept(visitor));
		visitor.leave(this);
	}

	/**
	 * A hook for interfere with the visitation of child elements.
	 *
	 * @param child The current child element
	 */
	protected Visitable prepareVisit(T child) {
		return child;
	}
}
