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

import java.util.function.Supplier;

import org.neo4j.driver.Driver;
import org.neo4j.driver.StatementResult;
import org.neo4j.driver.StatementRunner;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionUtils;

/**
 * Default implementation of {@link Neo4jOperations}. Uses the Neo4j Java driver to connect to and interact with the
 * database.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
public class Neo4jTemplate implements Neo4jOperations {

	private Supplier<StatementRunner> statementRunnerSupplier;

	public Neo4jTemplate(Driver driver) {
		this(() -> Neo4jTransactionUtils.retrieveTransactionalStatementRunner(driver));
	}

	Neo4jTemplate(Supplier<StatementRunner> statementRunnerSupplier) {
		this.statementRunnerSupplier = statementRunnerSupplier;
	}

	@Override
	public Object executeQuery(String query) {

		// TODO Let's see whether we can stick with the statementrunner or if we need to differentiate between reactive runner and default. Current 2.0 react version has two complete separate interfaces
		StatementRunner statementRunner = statementRunnerSupplier.get();
		StatementResult result = statementRunner.run(query);
		return result.list();
	}
}
