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
package org.neo4j.opencypherdsl;

import static org.apiguardian.api.API.Status.*;

import org.apiguardian.api.API;
import org.neo4j.opencypherdsl.support.Visitable;

/**
 * Expresses the length of a relationship.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = INTERNAL, since = "1.0")
public final class RelationshipLength implements Visitable {

	private final Integer minimum;

	private final Integer maximum;

	private final boolean unbounded;

	RelationshipLength() {
		this.minimum = null;
		this.maximum = null;
		this.unbounded = true;
	}

	RelationshipLength(Integer minimum, Integer maximum) {
		this.minimum = minimum;
		this.maximum = maximum;
		this.unbounded = false;
	}

	public Integer getMinimum() {
		return minimum;
	}

	public Integer getMaximum() {
		return maximum;
	}

	public boolean isUnbounded() {
		return unbounded;
	}
}
