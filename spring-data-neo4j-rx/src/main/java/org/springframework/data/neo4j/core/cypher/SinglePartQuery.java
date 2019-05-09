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

import org.apiguardian.api.API;
import org.springframework.data.neo4j.core.cypher.Statement.SingleQuery;
import org.springframework.data.neo4j.core.cypher.support.Visitable;
import org.springframework.data.neo4j.core.cypher.support.Visitor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * See <a href="https://s3.amazonaws.com/artifacts.opencypher.org/M14/railroad/SinglePartQuery.html">SinglePartQuery</a>.
 *
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class SinglePartQuery implements SingleQuery {

	private @Nullable final ReadingClause readingClause;

	private @Nullable final UpdatingClause updatingClause;

	private @Nullable final Return aReturn;

	/**
	 * Creates a single part query consisting of a return clause with an optional reading clause
	 *
	 * @param aReturn       The expressions to return
	 * @param readingClause The optional reading clause
	 * @return
	 */
	static SinglePartQuery createReturningQuery(Return aReturn, @Nullable ReadingClause readingClause) {

		Assert.notNull(aReturn, "A return clause is required.");

		return new SinglePartQuery(readingClause, null, aReturn);
	}

	/**
	 * Creates a single part query representing an update of some kind. Updates have an optional return clause.
	 *
	 * @param readingClause  The expressions to match
	 * @param updatingClause The expressions to update
	 * @param aReturn        The expressions to return, optional
	 * @return
	 */
	static SinglePartQuery createUpdatingQuery(ReadingClause readingClause, UpdatingClause updatingClause,
		@Nullable Return aReturn) {

		Assert.notNull(readingClause, "The reading clause is required.");
		Assert.notNull(updatingClause, "The update clause is required.");

		return new SinglePartQuery(readingClause, updatingClause, aReturn);
	}

	private SinglePartQuery(@Nullable ReadingClause readingClause, @Nullable UpdatingClause updatingClause,
		@Nullable Return aReturn) {
		this.readingClause = readingClause;
		this.updatingClause = updatingClause;
		this.aReturn = aReturn;
	}

	@Override
	public void accept(Visitor visitor) {

		Visitable.visitIfNotNull(readingClause, visitor);
		Visitable.visitIfNotNull(updatingClause, visitor);
		Visitable.visitIfNotNull(aReturn, visitor);
	}
}
