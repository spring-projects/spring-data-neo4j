/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.rest.graphdb;

import org.junit.*;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.helpers.Predicate;
import org.neo4j.kernel.Traversal;
import org.neo4j.rest.graphdb.MatrixDataGraph.RelTypes;
import org.neo4j.test.ImpermanentGraphDatabase;
import org.neo4j.test.TestGraphDatabaseFactory;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.neo4j.helpers.collection.IteratorUtil.addToCollection;
import static org.neo4j.helpers.collection.IteratorUtil.asCollection;


/**
 * TestClass for the MatrixDatabase
 * @author Klemens Burchardi
 * @since 03.08.11
 */
public class MatrixDatabaseTest {
	private static GraphDatabaseService graphDb;
	private static MatrixDataGraph mdg;
    private Transaction tx;

    @BeforeClass
	      public static void beforeClass() {
                graphDb =  new TestGraphDatabaseFactory().newImpermanentDatabase();
                mdg = new MatrixDataGraph(graphDb).createNodespace();
	      }

	      @AfterClass
	      public static void afterClass() {
	          graphDb.shutdown();
	      }


    @Before
    public void setUp() throws Exception {
        tx = mdg.getGraphDatabase().beginTx();
    }

    @After
    public void tearDown() throws Exception {
        tx.success();
        tx.close();
    }

    @Test
           public void checkNeoProperties() throws Exception {
        	   Node neoNode = mdg.getNeoNode();    
        	   boolean isSetupCorrectly = false;
        	   if (neoNode.getProperty("age").equals(29)  &&
        		   neoNode.getProperty("name").equals("Thomas Anderson")){
        		   isSetupCorrectly = true;
        	   }        	   
        	   assertTrue(isSetupCorrectly);
           }
           
           /**
            * get the number of all nodes that know the neo node
            * @throws Exception
            */
           @Test
           public void getNeoFriends() throws Exception {
               Node neoNode = mdg.getNeoNode();             
               Traverser friendsTraverser = getFriends( neoNode );
               int numberOfFriends = 0;              
               for ( Path friendPath : friendsTraverser ) {               
                   numberOfFriends++;                 
               }
              
               assertEquals( 4, numberOfFriends );
           }
           
           /**
            * get the number of all heroes that are connected to the heroes collection node
            * @throws Exception
            */
           @Test
           public void checkNumberOfHeroes() throws Exception {                         
               Traverser heroesTraverser = getHeroes();
               int numberOfHeroes = 0;              
               for ( Path heroPath : heroesTraverser ) {               
            	   numberOfHeroes++;                 
               }
              
               assertEquals( 3, numberOfHeroes );
           }

           
           @Test
           public void checkForIndex() throws Exception {
               IndexManager index = graphDb.index();
               assertTrue(index.existsForNodes("heroes"));
           }
           
           @Test
           public void checkForHeroesCollection() throws Exception {
               Node heroesCollectionNode = mdg.getHeroesCollectionNode();
               assertEquals( "Heroes Collection", heroesCollectionNode.getProperty("type") );
           }
           
           @Test
           public void useMorpheusQuery() throws Exception {
        	   IndexManager index = graphDb.index();        	   
        	   Index<Node> goodGuys = index.forNodes("heroes");
        	   for (Node morpheus : goodGuys.query("name", "Morpheus")){
        		   assertEquals( "Morpheus", morpheus.getProperty("name") );
        	   }
           }
           
           @Test
           public void useTrinityIndex() throws Exception {
        	   IndexManager index = graphDb.index();        	   
        	   Index<Node> goodGuys = index.forNodes("heroes");
        	   IndexHits<Node> hits = goodGuys.get( "name", "Trinity" );
        	   Node trinity = hits.getSingle();
        	   assertEquals( "Trinity", trinity.getProperty("name") );
           }
           
           @Test
           public void compareIndexAndTraversal() throws Exception {
        	   IndexManager index = graphDb.index();        	   
        	   Index<Node> goodGuys = index.forNodes("heroes");
        	   IndexHits<Node> hits = goodGuys.query( "name", "*" );        	  
        	   Traverser heroesTraverser = getHeroes();               
               assertEquals( addToCollection(heroesTraverser.nodes(), new HashSet<Node>()), addToCollection(hits.iterator() , new HashSet<Node>()));
           }
           
           
           @Test
           public void checkTraverseByProperties() throws Exception {    	  
               Traverser heroesTraverser = getHeroes();
               Traverser heroesTraverserByProperties = getHeroesByNodeProperties();
               assertEquals( heroesTraverser.nodes().iterator().next(), heroesTraverserByProperties.nodes().iterator().next() );
           }
          
           
           /**
            * returns a traverser for all nodes that have an outgoing relationship of the type KNOWS
            * @param person the startnode
            * @return the Traverser
            */
           private static Traverser getFriends( final Node person ) {
                    TraversalDescription td = graphDb.traversalDescription()
                            .breadthFirst()                            
                            .relationships( RelTypes.KNOWS, Direction.OUTGOING )
                            .evaluator( Evaluators.excludeStartPosition() );
                    return td.traverse( person );
           }
           
           /**
            * returns a traverser for all nodes that have an outgoing relationship of the type HERO          
            * @return the Traverser
            */
           private static Traverser getHeroes() {
                    TraversalDescription td =  graphDb.traversalDescription()
                            .breadthFirst()                            
                            .relationships( RelTypes.HERO, Direction.OUTGOING )
                            .evaluator( Evaluators.excludeStartPosition() );
                    return td.traverse( mdg.getHeroesCollectionNode() );
           }

        
           /**
            * returns a traverser for all nodes that have a property type == hero in the embedded Database
            * @return the Traverser
            */
           private Traverser getHeroesByNodeProperties() {
         	  TraversalDescription td =  graphDb.traversalDescription()
                       .breadthFirst()                        
                       .relationships( RelTypes.PERSONS_REFERENCE, Direction.OUTGOING )
                       .relationships( RelTypes.HEROES_REFERENCE, Direction.OUTGOING )
                       .relationships( RelTypes.HERO, Direction.OUTGOING )
                       .evaluator(Evaluators.excludeStartPosition())
                       .evaluator(new Evaluator() {
                           public Evaluation evaluate(Path path) {
                               return path.endNode().getProperty("type", "none").equals("hero") ? Evaluation.INCLUDE_AND_PRUNE : Evaluation.EXCLUDE_AND_CONTINUE;
                           }
                       });
         	 return td.traverse(mdg.getReferenceNode());
           }
           
           /**
            * checks if neo has a friend named cypher
            * @throws Exception
            */
           @Test
           public void findCypher() throws Exception{
        	   Node neoNode = mdg.getNeoNode();              
               Traverser friendsTraverser = getFriends( neoNode );
               boolean foundCypher = false;
               for ( Path friendPath : friendsTraverser ) {            	  
            	   if (friendPath.endNode().getProperty("name").equals("Cypher")){
            		   foundCypher = true;
            		   break;
            	   }
               }
               assertTrue(foundCypher);
           } 
           
           /**
            * get all nodes that have an outgoing CODED_BY relationship
            * @throws Exception
            */
           @Test
           public void getMatrixHackers() throws Exception
           {

                   Traverser traverser = findHackers( mdg.getNeoNode() );
                   int numberOfHackers = 0;
                   for ( Path hackerPath : traverser ) {
                       numberOfHackers++;
                   }
                   assertEquals( 1, numberOfHackers );
           }

          
           /**
            * returns a traverser for all nodes that have an outgoing CODED_BY relationship
            * based on a startnode
            * @param startNode the node to start from
            * @return the 'Traverser
            */
           private static Traverser findHackers( final Node startNode ) {
                    TraversalDescription td =  graphDb.traversalDescription()
                            .breadthFirst()
                            .relationships( RelTypes.CODED_BY, Direction.OUTGOING )
                            .relationships( RelTypes.KNOWS, Direction.OUTGOING )
                            .evaluator(
                                    Evaluators.includeWhereLastRelationshipTypeIs(RelTypes.CODED_BY) );
                    return td.traverse( startNode );
                }

	      


}
