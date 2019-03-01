/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core;

import org.apiguardian.api.API;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;

/**
 * Default implementation of {@link Neo4jOperations}. Uses the Neo4j Java driver to connect to and interact with the
 * database.
 *
 * @author Gerrit Meier
 */
public class Neo4jTemplate implements Neo4jOperations {

	private final Driver driver;

	@API(status = API.Status.STABLE, since = "1.0")
	public Neo4jTemplate(Driver driver) {
		this.driver = driver;
	}

	@Override
	public Object executeQuery(String query) {
		Session session = driver.session();
		Transaction transaction = session.beginTransaction();
		try {
			StatementResult result = transaction.run(query);
			transaction.success();
			return result.list();
		} catch (Exception e) {
			transaction.failure();
		} finally {
			transaction.close();
		}

		return null;
	}
}
