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
package org.springframework.data.neo4j.repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.model.Car;
import org.springframework.data.neo4j.model.Toyota;
import org.springframework.data.neo4j.model.Volvo;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;
import static org.junit.Assert.*;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/repository/GraphRepositoryTest-context.xml"})
public class PolymorphicRepositoryTest {	
	
	@Autowired
    private Neo4jTemplate neo4jTemplate;
	
	@Autowired
	private CarRepository carRepository;
	
    @BeforeTransaction
    public void cleanDb() {
        Neo4jHelper.cleanDb(neo4jTemplate);
    }
    
    @Test
    @Transactional
    public void testDesendantsAreSavedToBaseRepository() {
    	Toyota toyota = carRepository.save(new Toyota());
    	Volvo volvo = carRepository.save(new Volvo());    	    	
    	assertThat(asCollection(carRepository.findAll()), hasItems(volvo, toyota));    	
    }
    
    @Test
    @Transactional
    public void testDescendantHasConcreteTypeWhenRetrievedFromBaseRepository() {
    	Toyota toyota = carRepository.save(new Toyota());
    	Car car = carRepository.findOne(toyota.id);
    	assertTrue(car instanceof Toyota);
    }

}
