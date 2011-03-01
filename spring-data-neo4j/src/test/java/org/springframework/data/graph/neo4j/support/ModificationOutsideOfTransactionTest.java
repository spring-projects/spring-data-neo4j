package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.graph.neo4j.Person;
import static org.springframework.data.graph.neo4j.Person.persistedPerson;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.finder.NodeFinder;
import org.springframework.data.graph.neo4j.support.node.Neo4jHelper;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;

import static org.junit.Assert.*;
import static org.springframework.data.graph.neo4j.support.HasRelationshipMatcher.hasRelationship;
import static org.springframework.data.graph.neo4j.support.HasRelationshipMatcher.hasNoRelationship;

@RunWith( SpringJUnit4ClassRunner.class )
@ContextConfiguration( locations = {"classpath:org/springframework/data/graph/neo4j/support/Neo4jGraphPersistenceTest-context.xml"} )

public class ModificationOutsideOfTransactionTest
{

    protected final Log log = LogFactory.getLog( getClass() );

    @Autowired
    private GraphDatabaseContext graphDatabaseContext;

    @Autowired
    private FinderFactory finderFactory;

    @Before
    public void cleanDb() {
        Neo4jHelper.cleanDb(graphDatabaseContext);
    }

    @Test
    public void testCreateOutsideTransaction()
    {
        Person p = persistedPerson("Michael", 35);
        assertEquals( 35, p.getAge() );
        assertTrue( hasUnderlyingNode( p ) );
    }

    @Test
    public void subgraphCreatedOutsideOfTransactionShouldNotBePersisted()
    {
        Person michael = persistedPerson("Michael", 35);
        Person emil = persistedPerson("Emil", 35);

        michael.setBoss( emil );

        assertEquals( emil, michael.getBoss() );
        assertTrue( hasUnderlyingNode( michael ) );
        assertThat( nodeFor( michael ), hasNoRelationship( "boss", emil.getPersistentState() ) );
    }

    @Test
    public void testSetPropertyOutsideTransaction()
    {
        Person p = createPersonInTransaction( "Michael", 35 );
        p.setAge( 25 );
        assertEquals( 25, p.getAge() );
        assertEquals( 35, nodeFor( p ).getProperty( "Person.age" ) );
    }

    @Test
    public void shouldNotCreateGraphRelationshipOutsideTransaction()
    {
        Person p = createPersonInTransaction( "Michael", 35 );
        Person spouse = createPersonInTransaction( "Tina", 36 );

        p.setSpouse( spouse );

        assertEquals( spouse, p.getSpouse() );
        assertThat( nodeFor( p ), hasNoRelationship( "Person.spouse",spouse.getPersistentState() ) );


        Person spouse2 = createPersonInTransaction( "Rana", 5 );
        p.setSpouse( spouse2 );
        assertEquals( spouse2, p.getSpouse() );
    }

    @Test
    public void testCreateRelationshipOutsideTransactionAndPersist()
    {
        Person p = createPersonInTransaction( "Michael", 35 );
        Person spouse = createPersonInTransaction( "Tina", 36 );

        p.setSpouse( spouse );
        p.persist();

        assertEquals( spouse, p.getSpouse() );
        assertThat( nodeFor( p ), hasRelationship( "Person.spouse" ) );


        Person spouse2 = createPersonInTransaction( "Rana", 5 );
        p.setSpouse( spouse2 );
        assertEquals( spouse2, p.getSpouse() );
    }

    private Person createPersonInTransaction( String name, int age )
    {
        Transaction tx = graphDatabaseContext.beginTx();
        Person p = null;
        try
        {
            p = persistedPerson(name, age);
            tx.success();
        } finally
        {
            tx.finish();
        }
        return p;
    }

    private boolean hasUnderlyingNode( Person person )
    {
        return person.hasUnderlyingNode();
    }

    private Node nodeFor( Person person )
    {
        return person.getPersistentState();
    }

    @Test
    public void testGetPropertyOutsideTransaction()
    {
        Person p = createPersonInTransaction( "Michael", 35 );
        assertEquals( "Wrong age.", 35, p.getAge() );
    }

    @Test
    public void testFindOutsideTransaction()
    {
        final NodeFinder<Person> finder = finderFactory.createNodeEntityFinder( Person.class );
        assertEquals( false, finder.findAll().iterator().hasNext() );
    }

}
