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

package org.springframework.data.neo4j.rest.support;

import org.junit.Assert;
import org.junit.Test;
import org.neo4j.graphdb.*;

import java.util.Date;

public class RestGraphDbTests extends RestTestBase {

    @Test
    public void testGetRefNode() {
        Node refNode = restGraphDatabase.createNode();
        Node nodeById = restGraphDatabase.getNodeById( refNode.getId() );
        Assert.assertEquals( refNode, nodeById );
    }

    @Test
    public void testCreateNode() {
        Node node = restGraphDatabase.createNode();
        Assert.assertEquals( node, restGraphDatabase.getNodeById( node.getId() ) );
    }

    @Test
    public void testCreateRelationship() {
        Node refNode = restGraphDatabase.createNode();
        Node node = restGraphDatabase.createNode();
        Relationship rel = refNode.createRelationshipTo( node, Type.TEST );
        Relationship foundRelationship = IsRelationshipToNodeMatcher.relationshipFromTo( refNode.getRelationships( Type.TEST, Direction.OUTGOING ), refNode, node );
        Assert.assertNotNull( "found relationship", foundRelationship );
        Assert.assertEquals( "same relationship", rel, foundRelationship );
        Assert.assertThat( refNode.getRelationships( Type.TEST, Direction.OUTGOING ), new IsRelationshipToNodeMatcher( refNode, node ) );
        Assert.assertThat( refNode.getRelationships( Direction.OUTGOING ), new IsRelationshipToNodeMatcher( refNode, node ) );
        Assert.assertThat( refNode.getRelationships( Direction.BOTH ), new IsRelationshipToNodeMatcher( refNode, node ) );
        Assert.assertThat( refNode.getRelationships( Type.TEST ), new IsRelationshipToNodeMatcher( refNode, node ) );
    }

    @Test
    public void testBasic() {
        Node refNode = restGraphDatabase.createNode();
        Node node = restGraphDatabase.createNode();
        Relationship rel = refNode.createRelationshipTo( node,
                DynamicRelationshipType.withName( "TEST" ) );
        rel.setProperty( "date", new Date().getTime() );
        node.setProperty( "name", "Mattias test" );
        refNode.createRelationshipTo( node,
                DynamicRelationshipType.withName( "TEST" ) );

        for ( Relationship relationship : refNode.getRelationships() ) {
            System.out.println( "rel prop:" + relationship.getProperty( "date", null ) );
            Node endNode = relationship.getEndNode();
            System.out.println( "node prop:" + endNode.getProperty( "name", null ) );
        }
    }

}
