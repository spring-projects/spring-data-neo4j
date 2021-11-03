/*
 * Copyright 2011-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.core.transaction;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.neo4j.core.DatabaseSelection;
import org.springframework.data.neo4j.core.UserSelection;
import org.springframework.util.StringUtils;

/**
 * @author Michael J. Simons
 */
class Neo4jTransactionUtilsTest {

	@CsvSource(nullValues = "n/a",
			delimiter = '|',
			value = {
					"n/a| dbA| n/a   | userA | There is already an ongoing Spring transaction for the default user of the default database, but you requested 'userA' of 'dbA'",
					"dbA| n/a| userA | n/a   | There is already an ongoing Spring transaction for 'userA' of 'dbA', but you requested the default user of the default database",
					"dbA| dbB| userA | userB | There is already an ongoing Spring transaction for 'userA' of 'dbA', but you requested 'userB' of 'dbB'"
			}
	)
	@ParameterizedTest
	void formatOngoingTxInAnotherDbErrorMessageShouldWork(String cdb, String rdb, String cu, String ru, String expected) {

		DatabaseSelection currentDatabaseSelection = StringUtils.hasText(cdb) ?
				DatabaseSelection.byName(cdb) :
				DatabaseSelection.undecided();
		DatabaseSelection requestedDatabaseSelection = StringUtils.hasText(rdb) ?
				DatabaseSelection.byName(rdb) :
				DatabaseSelection.undecided();
		UserSelection currentUserSelection = StringUtils.hasText(cu) ?
				UserSelection.impersonate(cu) :
				UserSelection.connectedUser();
		UserSelection requestedUserSelection = StringUtils.hasText(ru) ?
				UserSelection.impersonate(ru) :
				UserSelection.connectedUser();

		String result = Neo4jTransactionUtils.formatOngoingTxInAnotherDbErrorMessage(
				currentDatabaseSelection, requestedDatabaseSelection, currentUserSelection, requestedUserSelection
		);
		assertThat(result).isEqualTo(expected);
	}
}
