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
package org.springframework.data.neo4j.annotation.graphproperty;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:graphproperty-annotation-test-context.xml"})
@Transactional
public class GraphPropertyAnnotationTests {
	@Autowired
	Neo4jTemplate neo4jTemplate;
	
    @Autowired
    ArtistRepository artistRepository;

    @Test
    public void shouldSaveANodeWithRenamedPropertyNames() throws Exception {
    	Calendar born = Calendar.getInstance();
    	Calendar died = Calendar.getInstance();
    	
    	born.set(1853, Calendar.MARCH, 30, 0, 0, 0);
    	died.set(1890, Calendar.JULY , 29, 0, 0, 0);
    	
    	Artist vanGogh = new Artist("Vincent", "Willem", "Van Gogh", born.getTime(), died.getTime());
    	
    	Artist vanGoghSavedNode = artistRepository.save(vanGogh);
    	Artist vanGoghRetrievedNode = artistRepository.findOne(vanGoghSavedNode.getId());
    	
    	assertEquals("Vincent", vanGoghRetrievedNode.getFirstName());
    	assertEquals("Willem", vanGoghRetrievedNode.getSecondName());
    	assertEquals("Van Gogh", vanGoghRetrievedNode.getLastName());
    	assertEquals(born.getTime(), vanGoghRetrievedNode.getBorn());
    	assertEquals(died.getTime(), vanGoghRetrievedNode.getDied());
    	
    	Node node = this.neo4jTemplate.getNode(vanGoghSavedNode.getId());
    	
    	assertEquals("Vincent", node.getProperty("first_name"));
    	assertEquals("Willem", node.getProperty("second_name"));
    	assertEquals("Van Gogh", node.getProperty("last_name"));
    	assertEquals(born.getTimeInMillis(), Long.parseLong((String) node.getProperty("born")));
    	assertEquals(died.getTimeInMillis(), Long.parseLong((String) node.getProperty("died")));
    }
}
