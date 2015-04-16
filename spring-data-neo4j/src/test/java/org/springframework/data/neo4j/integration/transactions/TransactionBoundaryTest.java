/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.integration.transactions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.integration.transactions.service.WrapperService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @see DATAGRAPH-602
 *
 * @author: Vince Bickers
 */
@ContextConfiguration(classes = {ApplicationConfig.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class TransactionBoundaryTest {

    @Autowired
    WrapperService wrapperService;

    @Before
    public void tearDown() {
        wrapperService.purge();
    }

    @Test
    public void shouldRollbackNestedTransactions() {
        try {
            wrapperService.composeSuccessThenFail();
            fail("should have thrown exception");
        } catch (Exception e) {
            assertEquals(0, countNodes());
        }
    }

    @Test
    public void shouldCommitNestedTransactions() {
        try {
            wrapperService.composeSuccessThenSuccess();
            assertEquals(2, countNodes());
        } catch (Exception e) {
            fail("should not have thrown exception");
        }

    }

    @Test
    public void shouldAlwaysCommitIfForced() {
        try {
            wrapperService.composeForceThenFail();
            fail("should have thrown exception");
        } catch (Exception e) {
            assertEquals(1, countNodes());
        }
    }


    private int countNodes() {
        Iterator iterator = wrapperService.loadNodes().iterator();
        int i = 0;
        while (iterator.hasNext()) {
            iterator.next();
            i++;
        }
        return i;
    }
}
