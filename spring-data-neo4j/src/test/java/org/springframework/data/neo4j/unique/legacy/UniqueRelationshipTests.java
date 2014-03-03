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
package org.springframework.data.neo4j.unique.legacy;

import org.junit.Test;
import org.neo4j.graphdb.*;
import org.springframework.data.neo4j.mapping.Neo4jPersistentTestBase;
import org.springframework.data.neo4j.model.BestFriend;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.neo4j.helpers.collection.MapUtil.map;

/**
 * @author mh
 * @since 18.04.12
 */
public class UniqueRelationshipTests extends Neo4jPersistentTestBase {

    @Override
    protected void setBasePackage(Neo4jMappingContext mappingContext) throws ClassNotFoundException {
        super.setBasePackage(mappingContext,Person.class.getPackage().getName());
    }

    @Test
    public void testCreateUniqueRelationship() {
        final Person p1 = storeInGraph(michael);
        final Person p2 = storeInGraph(andres);
        final BestFriend bestFriend = new BestFriend(p1, p2, "cypher");
        template.save(bestFriend);

        final Relationship bestFriendRel = template.getPersistentState(bestFriend);
        assertEquals(bestFriendRel,((Node)template.getPersistentState(michael)).getSingleRelationship(DynamicRelationshipType.withName("BEST_FRIEND"), Direction.OUTGOING));
        assertEquals(bestFriendRel.getEndNode(), template.getPersistentState(andres));
        assertEquals("cypher",bestFriendRel.getProperty("secretName"));
        assertEquals(bestFriendRel, getBestFriend());

        final Person p3 = storeInGraph(emil);
        final BestFriend bestFriend2 = new BestFriend(p1, p3, "cypher");
        template.save(bestFriend2);

        final Relationship bestFriend2Rel = template.getPersistentState(bestFriend2);
        assertEquals(bestFriend2Rel, bestFriendRel);
        assertEquals(bestFriend2Rel.getEndNode(), template.getPersistentState(andres));
        assertEquals(bestFriendRel, getBestFriend());
    }

    @Test
    public void testCreateUniqueRelationshipRelatedToVia() {
        final Person p1 = storeInGraph(michael);
        final Person p2 = storeInGraph(andres);
        p1.setBestFriend(p2,"cypher");
        template.save(p1);
        final BestFriend bestFriend = p1.getBestFriend();

        final Relationship bestFriendRel = template.getPersistentState(bestFriend);
        assertEquals(bestFriendRel,((Node)template.getPersistentState(michael)).getSingleRelationship(DynamicRelationshipType.withName("BEST_FRIEND"), Direction.OUTGOING));
        assertEquals(bestFriendRel.getEndNode(), template.getPersistentState(andres));
        assertEquals("cypher",bestFriendRel.getProperty("secretName"));
        assertEquals(bestFriendRel, getBestFriend());

        final Person p3 = storeInGraph(emil);
        p1.setBestFriend(p3,"cypher");
        final BestFriend bestFriend2 = p1.getBestFriend();
        template.save(bestFriend2);

        final Relationship bestFriend2Rel = template.getPersistentState(bestFriend2);
        assertEquals(bestFriend2Rel, bestFriendRel);
        assertEquals(bestFriend2Rel.getEndNode(), template.getPersistentState(andres));
        assertEquals(bestFriendRel, getBestFriend());

        p1.setBestFriend(null,null);
        template.save(p1);
        assertEquals(null, ((Node) template.getPersistentState(michael)).getSingleRelationship(DynamicRelationshipType.withName("BEST_FRIEND"), Direction.OUTGOING));
        p1.setBestFriend(p3, "cypher");
        template.save(p1);
        final BestFriend bestFriend3 = p1.getBestFriend();

        final Relationship bestFriend3Rel = template.getPersistentState(bestFriend3);
        assertNotSame(bestFriend3Rel, bestFriendRel);
        assertEquals(bestFriend3Rel.getEndNode(), template.getPersistentState(emil));
        assertEquals(bestFriend3Rel, getBestFriend());
    }

    private Relationship getBestFriend() {
        return template.<Relationship,BestFriend>getIndex(BestFriend.class).get("secretName", "cypher").getSingle();
    }

    @Test
    public void testDeleteAndRecreateUniqueRelationship() {
        final Node n1 = template.createNode();
        final Node n2 = template.createNode();
        final Relationship r1 = template.getOrCreateRelationship("test", "key", "value", n1, n2, "type", null);
        template.delete(r1);
        final Node n3 = template.createNode();
        final Relationship r2 = template.getOrCreateRelationship("test", "key", "value", n1, n3, "type", null);
        assertFalse("r1 is returned although being deleted", r1.equals(r2));
    }
}
