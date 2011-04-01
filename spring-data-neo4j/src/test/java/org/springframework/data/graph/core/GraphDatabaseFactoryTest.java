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

package org.springframework.data.graph.core;

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.graph.neo4j.support.DelegatingGraphDatabase;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author mh
 * @since 29.03.11
 */
public class GraphDatabaseFactoryTest {

    @Test
    public void shouldCreateLocalDatabaseFromContext() throws Exception {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("GraphDatabaseFactory-context.xml");
        try {
            GraphDatabase graphDatabase = ctx.getBean("graphDatabase", GraphDatabase.class);
            assertThat(graphDatabase, is(not(nullValue())));
            assertThat(graphDatabase, is(instanceOf(DelegatingGraphDatabase.class)));
        } finally {
            ctx.close();
        }

    }
    @Test
    public void shouldCreateLocalDatabase() throws Exception {
        GraphDatabaseFactory factory = new GraphDatabaseFactory();
        try {
            factory.setStoreLocation("target/test-db");
            GraphDatabase graphDatabase = factory.getObject();
            assertThat(graphDatabase, is(not(nullValue())));
            assertThat(graphDatabase,is(instanceOf(DelegatingGraphDatabase.class)));
        } finally {
            factory.shutdown();
        }
    }
}
