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
package org.springframework.data.neo4j.typerepresentation;

import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.support.MappingInfrastructureFactoryBean;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.ReferenceNodes;
import org.springframework.data.neo4j.support.typerepresentation.TypeRepresentationStrategyFactory;

public class TypeRepresentationTests {
    @Test
    public void testSavingTwiceResultsOnlyInOneTRSCall() throws Exception {
        GraphDatabaseService db = new TestGraphDatabaseFactory().newImpermanentDatabase();
        ReferenceNodes.obtainReferenceNode(db,"root");
        MappingInfrastructureFactoryBean factoryBean = new MappingInfrastructureFactoryBean(db, null);
        factoryBean.setTypeRepresentationStrategy(TypeRepresentationStrategyFactory.Strategy.SubRef);
        factoryBean.afterPropertiesSet();
        Neo4jTemplate template = new Neo4jTemplate(factoryBean.getObject());
        Transaction tx = db.beginTx();
        Person person = template.save(new Person());
        person.setName("Bar");
        template.save(person);
        tx.failure();
        tx.close();
    }
}
