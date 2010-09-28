package org.springframework.datastore.graph.neo4j.partial;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.datastore.graph.neo4j.spi.node.Neo4jHelper;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/datastore/graph/neo4j/partial/Neo4jGraphRecommendationTest-context.xml"})
//@TransactionConfiguration(defaultRollback = false)
@Transactional
public class RecommendationTest {

    protected final Log log = LogFactory.getLog(getClass());

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    private GraphDatabaseContext graphDatabaseContext;

    @PersistenceContext
    EntityManager em;

    @Autowired
    DataSource dataSource;

    @Rollback(false)
    @Before
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
    public void jpaUserHasNodeAndId() {
        User user = new User();
        user.setAge(35);
        user = em.merge(user);
        Assert.assertNotNull(user.getId());
        Assert.assertNotNull(user.getUnderlyingState());
    }
    @Test
    public void jpaUserCanHaveGrapProperties() {
        User user = new User();
        user.setAge(35);
        user.setNickname("John");
        user = em.merge(user);
        Assert.assertNotNull(user.getId());
        Assert.assertNotNull(user.getUnderlyingState());
        Assert.assertNotNull(user.getNickname());
        Assert.assertEquals("John",user.getUnderlyingState().getProperty("nickname"));

    }
}
