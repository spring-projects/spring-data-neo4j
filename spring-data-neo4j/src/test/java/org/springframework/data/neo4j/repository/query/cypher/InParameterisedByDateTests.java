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
package org.springframework.data.neo4j.repository.query.cypher;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.repository.CRUDRepository;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.data.neo4j.repository.query.cypher.Dates.TODAY;
import static org.springframework.data.neo4j.repository.query.cypher.Dates.TOMORROW;
import static org.springframework.data.neo4j.repository.query.cypher.Dates.YESTERDAY;

class Dates {
    public static final Date TODAY = new Date();
    public static final Date TOMORROW = new Date(TODAY.getTime() + 1000 * 60 * 60 * 24);
    public static final Date YESTERDAY = new Date(TODAY.getTime() - 1000 * 60 * 60 * 24);
}

@NodeEntity
class DateEntity {
    @GraphId
    Long id;

    Date date;

    Date[] dates;

    DateEntity() {
    }

    DateEntity(Date date, Date[] dates) {
        this.date = date;
        this.dates = dates;
    }
}

interface DateEntityRepository extends CRUDRepository<DateEntity> {
    @Query("start n=node(*) where n.date in {0} return n")
    DateEntity findUsingDate(Date value);

    @Query("start n=node(*) where n.date in {0} return n")
    DateEntity findUsingArrayOfDate(Date... value);

    @Query("start n=node(*) where n.dates in {0} return n")
    DateEntity findUsingArrayOfArrayOfDate(Iterable<Date[]> value);
}

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class InParameterisedByDateTests {
    @Configuration
    @EnableNeo4jRepositories
    static class TestConfig extends Neo4jConfiguration {
        TestConfig() throws ClassNotFoundException {
            setBasePackage(DateEntity.class.getPackage().getName());
        }

        @Bean
        GraphDatabaseService graphDatabaseService() {
            return new TestGraphDatabaseFactory().newImpermanentDatabase();
        }
    }

    @Autowired
    GraphDatabaseService graphDatabaseService;

    @Autowired
    DateEntityRepository dateEntityRepository;

    @Before
    public void before() {
        Neo4jHelper.cleanDb(graphDatabaseService, true);

        Transaction transaction = graphDatabaseService.beginTx();

        try {
            dateEntityRepository.save(new DateEntity(TODAY, new Date[]{TODAY, TOMORROW}));

            transaction.success();
        } finally {
            transaction.finish();
        }
    }

    @Test
    public void shouldSupportSingleDateAsParameterForIn() throws Exception {
        assertThat(dateEntityRepository.findUsingDate(TODAY).date, is(TODAY));
    }

    @Test
    public void shouldSupportArrayOfDateAsParameterForIn() throws Exception {
        assertThat(dateEntityRepository.findUsingArrayOfDate(TODAY).date, is(TODAY));
        assertThat(dateEntityRepository.findUsingArrayOfDate(TODAY, TOMORROW).date, is(TODAY));
    }

    @Test
    public void shouldSupportArrayOfArrayOfDateAsParameterForIn() throws Exception {
        Iterable<Date[]> dates = asList(new Date[]{TODAY, TOMORROW}, new Date[]{YESTERDAY});
        assertThat(dateEntityRepository.findUsingArrayOfArrayOfDate(dates).date, is(TODAY));
    }
}
