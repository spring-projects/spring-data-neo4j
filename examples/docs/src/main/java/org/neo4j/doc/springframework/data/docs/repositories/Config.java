package org.neo4j.doc.springframework.data.docs.repositories;

// tag::java-config-imperative[]
import java.util.Arrays;
import java.util.Collection;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
// tag::java-config-imperative-short[]
import org.neo4j.springframework.data.config.AbstractNeo4jConfig;
import org.neo4j.springframework.data.repository.config.EnableNeo4jRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration // <1>
@EnableNeo4jRepositories // <2>
@EnableTransactionManagement // <3>
public class Config extends AbstractNeo4jConfig { // <4>

	@Bean
	public Driver driver() { // <5>
		return GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "secret"));
	}
	// end::java-config-imperative-short[]

	@Override
	protected Collection<String> getMappingBasePackages() { // <6>
		return Arrays.asList("your.domain.package");
	}

	// tag::java-config-imperative-short[]
}
// end::java-config-imperative[]
// end::java-config-imperative-short[]
