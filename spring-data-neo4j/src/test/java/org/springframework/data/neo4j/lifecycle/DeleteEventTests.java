/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedList;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;

@Deprecated
@NodeEntity
class DeprecatedProgram {
    @GraphId
    Long id;

    String name;

    DeprecatedProgram() {
    }

    public DeprecatedProgram(String name) {
        this.name = name;
    }
}

// This test should be deprecated moving forward as it is replaced by
// BeforeAndAfterDeleteEventTests, however as we are leaving the
// DeleteEvent class for backwards compatibility, we leave this test
// here too to ensure we don't break anything.
@Deprecated
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public class DeleteEventTests {
    @Configuration
    @EnableNeo4jRepositories
    static class TestConfig extends Neo4jConfiguration {
        TestConfig() throws ClassNotFoundException {
            setBasePackage(DeprecatedProgram.class.getPackage().getName());
        }
        @Bean
        GraphDatabaseService graphDatabaseService() {
            return new TestGraphDatabaseFactory().newImpermanentDatabase();
        }

        @Bean
        ApplicationListener<DeleteEvent<DeprecatedProgram>> deleteEventApplicationListener() {
            return new ApplicationListener<DeleteEvent<DeprecatedProgram>>() {
                @Override
                public void onApplicationEvent(DeleteEvent<DeprecatedProgram> event) {
                    deletions.add(event.getEntity());
                }
            };
        }
    }

    @Autowired
    Neo4jTemplate template;

    @Autowired
    GraphDatabaseService graphDatabaseService;

    static final LinkedList<DeprecatedProgram> deletions = new LinkedList<DeprecatedProgram>();

    @BeforeTransaction
    public void beforeTransaction() {
        Neo4jHelper.cleanDb(template);
    }

    @Before
    public void before() {
        deletions.clear();
    }

    @Test
    public void shouldFireEventOnNodeDeletion() throws Exception {
        DeprecatedProgram sark = template.save(new DeprecatedProgram("Sark"));

        template.delete(sark);

        assertThat(deletions, hasItem(sark));
    }
}
