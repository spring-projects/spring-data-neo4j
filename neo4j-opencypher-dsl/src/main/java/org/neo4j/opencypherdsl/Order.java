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

import java.util.List;

import org.apiguardian.api.API;
import org.neo4j.opencypherdsl.support.TypedSubtree;

/**
 * Represents the list of sort items that make up the order of records in a result set.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 * @param <S> The order's entry type
 * @since 1.0
 */
@API(status = EXPERIMENTAL, since = "1.0")
public final class Order<S extends Order<S>> extends TypedSubtree<SortItem, S> {

	Order(List<SortItem> sortItems) {
		super(sortItems);
	}
}
