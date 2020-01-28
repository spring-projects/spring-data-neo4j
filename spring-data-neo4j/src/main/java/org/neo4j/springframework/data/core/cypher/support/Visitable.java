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

import org.springframework.lang.Nullable;

/**
 * Interface for implementations that accepts {@link Visitor visitors}.
 *
 * @author Michael Simons
 * @see Visitor
 * @since 1.0
 */
public interface Visitable {

	/**
	 * A helper method that presents the {@code visitor} to the {@code visitable} if the visitable is not null.
	 * Not meant to be overridden.
	 *
	 * @param visitable The visitable to visit if not null
	 * @param visitor The visitor to use
	 */
	static void visitIfNotNull(@Nullable Visitable visitable, Visitor visitor) {

		if (visitable != null) {
			visitable.accept(visitor);
		}
	}

	/**
	 * Accept a {@link Visitor} visiting this {@link Visitable} and its nested {@link Visitable}s if applicable.
	 *
	 * @param visitor the visitor to notify, must not be {@literal null}.
	 */
	default void accept(Visitor visitor) {

		visitor.enter(this);
		visitor.leave(this);
	}
}
