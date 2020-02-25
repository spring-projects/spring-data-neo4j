package org.neo4j.doc.springframework.data.docs.repositories;

// tag::java-config-reactive[]
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.springframework.data.config.AbstractReactiveNeo4jConfig;
import org.neo4j.springframework.data.repository.config.EnableReactiveNeo4jRepositories;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableReactiveNeo4jRepositories
@EnableTransactionManagement
public class ReactiveConfig extends AbstractReactiveNeo4jConfig {

	@Bean
	public Driver driver() {
		return GraphDatabase.driver("bolt://localhost:7687", AuthTokens.basic("neo4j", "secret"));
	}
}
// end::java-config-reactive[]
