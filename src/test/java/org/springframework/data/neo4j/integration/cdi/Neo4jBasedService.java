package org.springframework.data.neo4j.integration.cdi;

import javax.inject.Inject;

import org.neo4j.driver.Driver;

public class Neo4jBasedService {

	@Inject Driver driver;

	@Inject
	PersonRepository personRepository;

	public Neo4jBasedService() {
		System.out.println("peng!!!");
	}
}
