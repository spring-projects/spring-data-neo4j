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

package org.springframework.data.neo4j.support;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.IsNot;
import org.hamcrest.core.IsNull;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.neo4j.core.GraphDatabase;

/**
 * @author mh
 * @since 29.03.11
 */
public class GraphDatabaseFactoryTests {

    @Test
    public void shouldCreateLocalDatabaseFromContext() throws Exception {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("GraphDatabaseFactory-context.xml");
        try {
            GraphDatabase graphDatabase = ctx.getBean("graphDatabase", GraphDatabase.class);
            Assert.assertThat(graphDatabase, Is.is(IsNot.not(IsNull.nullValue())));
            Assert.assertThat(graphDatabase, Is.is(IsInstanceOf.instanceOf(DelegatingGraphDatabase.class)));
        } finally {
            ctx.close();
        }

    }
    @Test
    public void shouldCreateLocalDatabase() throws Exception {
        GraphDatabaseFactoryBean factory = new GraphDatabaseFactoryBean();
        try {
            factory.setStoreLocation("target/test-db");
            GraphDatabase graphDatabase = factory.getObject();
            Assert.assertThat(graphDatabase, Is.is(IsNot.not(IsNull.nullValue())));
            Assert.assertThat(graphDatabase, Is.is(IsInstanceOf.instanceOf(DelegatingGraphDatabase.class)));
        } finally {
            factory.shutdown();
        }
    }
}
