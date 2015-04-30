/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and licence terms.  Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's licence, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.integration.movies.domain.queryresult;

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