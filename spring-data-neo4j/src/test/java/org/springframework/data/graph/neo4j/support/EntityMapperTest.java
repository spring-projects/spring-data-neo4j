package org.springframework.data.graph.neo4j.support;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.Person;
import org.springframework.data.graph.neo4j.support.EntityPath;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
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
public class EntityMapperTest {

    @Autowired
    private GraphDatabaseContext ctx;

    @Test
    @Transactional
    public void entityMapperShouldForwardEntityPath() throws Exception {
        Person michael = new Person("Michael", 36).persist();
        EntityMapper<Person, Person, String> mapper = new EntityMapper<Person, Person, String>(ctx) {
            @Override
            public String mapPath(EntityPath<Person, Person> entityPath) {
                return entityPath.<Person>startEntity().getName();
            }
        };
        String name = mapper.mapPath(new NodePath(michael.getPersistentState()));
        Assert.assertEquals(michael.getName(), name);
    }
}
