package com.springone.myrestaurants.domain;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.GraphDatabaseContext;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * @author Michael Hunger
 * @since 02.10.2010
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@DirtiesContext
@Transactional
public class TopRatedRestaurantFinderTest {
    @Autowired
    private GraphDatabaseContext graphDatabaseContext;

    @PersistenceContext
    EntityManager em;

    @Autowired
    DataSource dataSource;

    @Autowired
    PlatformTransactionManager transactionManager;

    @Before
    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
    public void returnsFriendsInOrder() {
        final Restaurant a = restaurant("a");
        final Restaurant b = restaurant("b");
        final Restaurant c = restaurant("c");
        final UserAccount A = user("A");
        final UserAccount B1 = user("B1");
        final UserAccount B2 = user("B2");
        final UserAccount C1 = user("C1");
        Assert.assertNotNull("user has node", node(A));
        A.knows(B1);
        A.knows(B2);
        B1.knows(C1);
        C1.rate(a, 1, "");
        C1.rate(b, 5, "");
        C1.rate(c, 3, "");
        final Node node = node(A);
        final Collection<RatedRestaurant> topNRatedRestaurants = new TopRatedRestaurantFinder().getTopNRatedRestaurants(A, 5);
        Collection<Restaurant> result = new ArrayList<Restaurant>();
        for (RatedRestaurant ratedRestaurant : topNRatedRestaurants) {
            result.add(ratedRestaurant.getRestaurant());
        }
        final Restaurant b2 = em.find(Restaurant.class, 2L);
        Assert.assertNotNull(b2);
        Assert.assertEquals(asList(b,c, a), result);
    }

    private Node node(UserAccount a) {
        return a.getPersistentState();
    }

    private UserAccount user(String name) {
        UserAccount userAccount = new UserAccount();
        //userAccount.setId((long) name.hashCode());
        em.persist(userAccount);
        em.flush();
        userAccount.persist();
        userAccount.setNickname(name);
        return userAccount;
    }

    private Restaurant restaurant(String name) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(name);
        //restaurant.setId((long) name.hashCode());
        em.persist(restaurant);
        em.flush();
        restaurant.persist();
        return restaurant;
    }

    private void dumpResults(String sql) {
        final List<Map<String, Object>> result = new SimpleJdbcTemplate(dataSource).queryForList(sql);
        System.out.println("result = " + result);
    }
}
