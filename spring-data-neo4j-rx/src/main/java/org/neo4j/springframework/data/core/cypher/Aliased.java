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
package org.neo4j.springframework.data.core.cypher;

import static org.apiguardian.api.API.Status.*;

import org.apiguardian.api.API;

/**
 * An element with an alias. An alias has a subtle difference to a symbolic name in cypher. Nodes and relationships can
 * have symbolic names which in turn can be aliased as well.
 * <p>
 * Therefor, the Cypher generator needs both {@code Named} and {@code Aliased}.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public interface Aliased {

	/**
	 * @return the alias.
	 */
	String getAlias();

	/**
	 * Turns this alias into a symbolic name that can be used as an {@link Expression}.
	 *
	 * @return A new symbolic name
	 */
	default SymbolicName asName() {
		return SymbolicName.create(this.getAlias());
	}
}
