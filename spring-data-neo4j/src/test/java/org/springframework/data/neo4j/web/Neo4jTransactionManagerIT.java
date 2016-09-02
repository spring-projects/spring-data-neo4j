package org.springframework.data.neo4j.web;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.ogm.testutil.MultiDriverTestClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.transactions.DelegatingTransactionManager;
import org.springframework.data.neo4j.web.context.WebAppContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author vince
 */
@ContextConfiguration(classes = {WebAppContext.class})
@WebAppConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class Neo4jTransactionManagerIT extends MultiDriverTestClass {

    @Autowired
    PlatformTransactionManager transactionManager;

    @Before
    public void setUp() {
        // does nothing
    }

    @Test
    @Transactional(readOnly = true)
    public void shouldCreateReadOnlyTransaction() {
        Assert.assertTrue(((DelegatingTransactionManager) transactionManager).getTransactionDefinition().isReadOnly());

    }

    @Test
    @Transactional(readOnly = false)
    public void shouldCreateReadWriteTransaction() {
        Assert.assertFalse(((DelegatingTransactionManager) transactionManager).getTransactionDefinition().isReadOnly());
    }

    @Test
    @Transactional
    public void shouldDefaultToReadWriteTransaction() {
        Assert.assertFalse(((DelegatingTransactionManager) transactionManager).getTransactionDefinition().isReadOnly());
    }

}
