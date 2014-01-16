package com.springdeveloper.data.neo;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;


public class PrintNeo4j {

	public static void main(String[] args) {
		System.out.println("Hello Neo4j!");
//		GraphDatabaseService graphDb = new EmbeddedGraphDatabase( "target/data/test" );
		GraphDatabaseService graphDb = new EmbeddedGraphDatabase( "/Users/trisberg/neo4j/imdb" );
		Transaction tx = graphDb.beginTx();

		tx = graphDb.beginTx();
		try {
			for (Node n : graphDb.getAllNodes()) {
				System.out.print("=> " + n.getId() + " :: ");
				for (String s : n.getPropertyKeys()) {
					System.out.print(" : " + s + " = " + n.getProperty(s));
				}
				for (Relationship r : n.getRelationships()) {
					long start = r.getStartNode().getId();
					long end = r.getEndNode().getId();
					if (n.getId() == start) {
						System.out.print(" ** " + r.getType().name() + "--> " + end);
						
					}
					if (n.getId() == end) {
						System.out.print(" ** " + start + " <--" + r.getType().name());
						
					}
					for (String s : r.getPropertyKeys()) {
						System.out.print(" :: " + s + " = " + r.getProperty(s));
					}
				}
				System.out.println("!");
			}
			tx.success();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			tx.close();
		}
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		graphDb.shutdown();
		System.out.println("Done!");
	}

}
