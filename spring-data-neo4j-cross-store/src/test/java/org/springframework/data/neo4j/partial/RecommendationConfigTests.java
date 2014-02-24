/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.partial;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.partial.model.User;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.CleanContextCacheTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.sql.DataSource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/partial/Neo4jGraphRecommendationTest-context-config.xml"})
@TestExecutionListeners({CleanContextCacheTestExecutionListener.class, DependencyInjectionTestExecutionListener.class, TransactionalTestExecutionListener.class})
public class RecommendationConfigTests {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    private Neo4jTemplate template;

    @PersistenceContext
    EntityManager em;

    @Autowired
    DataSource dataSource;

    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(template);
    }

    @Test
    @Transactional
    public void jpaUserHasNodeAndId() {
        User user = user("John");
        Assert.assertNotNull("jpa-id", user.getId());
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
