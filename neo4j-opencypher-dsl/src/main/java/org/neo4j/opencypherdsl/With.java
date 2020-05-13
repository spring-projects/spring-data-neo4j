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
import org.neo4j.opencypherdsl.support.Visitor;

/**
 * See <a href="https://s3.amazonaws.com/artifacts.opencypher.org/railroad/With.html">With</a>.
 *
 * @author Michael J. Simons
 * @soundtrack Ferris MC - Ferris MC's Audiobiographie
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public final class With implements Visitable {

	private final Distinct distinct;

	private final ReturnBody body;

	private final Where where;

	With(boolean distinct, ExpressionList returnItems, Order order, Skip skip, Limit limit, Where where) {
		this.distinct = distinct ? Distinct.INSTANCE : null;
		this.body = new ReturnBody(returnItems, order, skip, limit);
		this.where = where;
	}

	@Override
	public void accept(Visitor visitor) {

		visitor.enter(this);
		Visitable.visitIfNotNull(this.distinct, visitor);
		this.body.accept(visitor);
		Visitable.visitIfNotNull(where, visitor);
		visitor.leave(this);
	}
}
