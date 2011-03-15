package org.springframework.data.graph.neo4j.partial;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/partial/Neo4jGraphRecommendationTest-context.xml"})
//@Ignore("seems to break things in the graph store")
@DirtiesContext
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

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
    @Transactional
    public void jpaUserHasNodeAndId() {
        User user = user("John");
        Assert.assertNotNull("jpa-id",user.getId());
        Assert.assertNotNull("node",user.getPersistentState());
    }
    @Test
    @Transactional
    public void jpaUserCanHaveGraphProperties() {
        User user = user("John");
        Assert.assertNotNull("jpa-id",user.getId());
        Assert.assertNotNull("node",user.getPersistentState());
        Assert.assertNotNull("nickname in entity",user.getNickname());
        Assert.assertEquals("nickname in graph","John",user.getPersistentState().getProperty("nickname"));
    }

    private User user(final String name) {
        User user = new User();
        user.setAge(35);
        user.setNickname(name);
        em.persist(user);
        em.flush();
        return user.persist();
    }

    @Test
    @Transactional
    public void jpaUserCanHaveGraphRelationships() {
        User user = user("John");
        Assert.assertNotNull("jpa-id",user.getId());
        Assert.assertNotNull("node",user.getPersistentState());
        User user2 = user("Jane");
        user.getFriends().add(user2);
        //user.knows(user2);
        Assert.assertEquals(user2, user.getFriends().iterator().next());
    }
}
