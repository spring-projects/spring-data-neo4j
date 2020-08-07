package org.springframework.data.neo4j.integration.cdi;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

/**
 * @author Michael J. Simons
 * @soundtrack Juse Ju - Millennium
 */
@ApplicationScoped
public class Neo4jProducers {

	public Neo4jProducers() {
		System.out.println("constru");
	}

	@Produces
	@Singleton
	public Driver driver() {
		System.out.println("peng");
		return GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "secret"));
	}



}
