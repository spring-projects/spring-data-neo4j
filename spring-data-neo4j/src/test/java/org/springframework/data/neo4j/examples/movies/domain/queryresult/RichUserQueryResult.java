/*
 * Copyright 2011-2019 the original author or authors.
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

package org.springframework.data.neo4j.examples.movies.domain.queryresult;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.springframework.data.neo4j.annotation.QueryResult;

/**
 * A {@link QueryResult} that contains rich types.
 *
 * @author Adam George
 * @author Luanne Misquitta
 */
@QueryResult
public class RichUserQueryResult {

	private Gender userGender; // should be handled by default type conversion
	private String userName;
	private BigInteger userAccount;
	private BigDecimal[] userDeposits;

	public Gender getUserGender() {
		return userGender;
	}

	public String getUserName() {
		return userName;
	}

	public BigInteger getUserAccount() {
		return userAccount;
	}

	public BigDecimal[] getUserDeposits() {
		return userDeposits;
	}
}
