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
import org.neo4j.springframework.data.core.cypher.support.Visitable;
import org.neo4j.springframework.data.core.cypher.support.Visitor;
import org.springframework.util.Assert;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public final class Limit implements Visitable {

	static Limit create(Number value) {

		Assert.notNull(value, "A limit cannot have a null value.");

		return new Limit(new NumberLiteral(value));
	}

	private final NumberLiteral limitAmount;

	private Limit(NumberLiteral limitAmount) {
		this.limitAmount = limitAmount;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.enter(this);
		limitAmount.accept(visitor);
		visitor.leave(this);
	}
}
