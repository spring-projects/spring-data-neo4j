package com.springone.myrestaurants.data;

import com.springone.myrestaurants.domain.Restaurant;
import junit.framework.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
public class RestaurantRepositoryTest {

    protected final Log log = LogFactory.getLog(getClass());

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    private Neo4jTemplate template;

    @PersistenceContext
    EntityManager em;
    
    @Autowired
    RestaurantRepository repo;

    @Transactional
    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(template);
    }

    @Transactional
    @Test
    public void testFindRestaurant() {
    	Restaurant r = repo.findRestaurant(1L);
    	Assert.assertNotNull("should have found something" ,r);
    	Assert.assertEquals("should have found the right one", "Boston Market", r.getName());
    }

    @Transactional
    @Test
    public void testFindAllRestaurants() {
    	List<Restaurant> l = repo.findAllRestaurants();
    	Assert.assertEquals("should have found them all", 50, l.size());
    }

    @Transactional
    @Test
    public void testFindRestaurantEntries() {
    	List<Restaurant> l = repo.findRestaurantEntries(20, 10);
    	Assert.assertEquals("should have found correct number of entries", 10, l.size());
    }

    @Transactional
    @Test
    public void testCountRestaurants() {
    	long l = repo.countRestaurants();
    	Assert.assertEquals("should be the correctnumber", 50, l);
    }
}
