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

import static java.util.stream.Collectors.*;

import java.util.ArrayList;
import java.util.List;

import org.apiguardian.api.API;
import org.neo4j.springframework.data.core.cypher.Statement.RegularQuery;
import org.neo4j.springframework.data.core.cypher.support.Visitor;
import org.springframework.util.Assert;

/**
 * @author Michael J. Simons
 * @since 1.0
 */
@API(status = API.Status.INTERNAL, since = "1.0")
public final class UnionQuery implements RegularQuery {

	static UnionQuery create(boolean unionAll, List<SingleQuery> queries) {

		Assert.isTrue(queries != null && queries.size() >= 2, "At least two queries are needed.");

		List<UnionPart> unionParts = queries.stream().skip(1).map(q -> new UnionPart(unionAll, q)).collect(toList());
		return new UnionQuery(unionAll, queries.get(0), unionParts);
	}

	private final boolean all;

	private final SingleQuery firstQuery;

	private final List<UnionPart> additionalQueries;

	private UnionQuery(boolean all, SingleQuery firstQuery, List<UnionPart> additionalQueries) {
		this.all = all;
		this.firstQuery = firstQuery;
		this.additionalQueries = additionalQueries;
	}

	/**
	 * Creates a new union query by appending more parts
	 *
	 * @param newAdditionalQueries more additional queries
	 * @return A new union query
	 */
	UnionQuery addAdditionalQueries(List<SingleQuery> newAdditionalQueries) {

		List<SingleQuery> queries = new ArrayList<>();
		queries.add(firstQuery);
		queries.addAll(additionalQueries.stream().map(UnionPart::getQuery).collect(toList()));
		queries.addAll(newAdditionalQueries);

		return create(this.isAll(), queries);
	}

	boolean isAll() {
		return all;
	}

	@Override
	public void accept(Visitor visitor) {

		visitor.enter(this);
		this.firstQuery.accept(visitor);
		this.additionalQueries.forEach(q -> q.accept(visitor));
		visitor.leave(this);
	}
}
