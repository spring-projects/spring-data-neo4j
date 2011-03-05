package org.springframework.data.graph.neo4j.support;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.Person;
import org.springframework.data.graph.neo4j.template.NodePath;
import org.springframework.data.graph.neo4j.transaction.EntityMapper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author mh
 * @since 26.02.11
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"})
public class EntityPathTest {

    @Autowired
    private GraphDatabaseContext ctx;

    @Test
    @Transactional
    public void shouldConvertNodePathToEntityPath() throws Exception {
        Person michael = new Person("Michael", 36).persist();
        Node node = michael.getPersistentState();
        NodePath path = new NodePath(node);
        EntityPath<Person, Person> entityPath = new EntityPath<Person, Person>(ctx, path);

        Assert.assertEquals("start entity",michael, entityPath.startEntity());
        Assert.assertEquals("start node",node, path.startNode());
        Assert.assertEquals("end entity",michael, entityPath.endEntity());
        Assert.assertEquals("end node",node, path.endNode());
        Assert.assertNull("no relationship", entityPath.lastRelationshipEntity());
        Assert.assertNull("no relationship", path.lastRelationship());

        // todo all 6 iterators
    }
}
