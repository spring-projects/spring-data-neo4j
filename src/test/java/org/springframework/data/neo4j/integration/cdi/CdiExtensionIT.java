package org.springframework.data.neo4j.integration.cdi;

import static org.assertj.core.api.Assertions.assertThat;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.neo4j.config.Neo4jCDIConfigurationSupport;
import org.springframework.data.neo4j.repository.support.SomeOtherExtension;

public class CdiExtensionIT {

	static SeContainer container;

	@BeforeAll
	static void setupTheThing() {

		container = SeContainerInitializer.newInstance() //
				.disableDiscovery() //
				.addBeanClasses(Neo4jCDIConfigurationSupport.class)
				.addExtensions(SomeOtherExtension.class)
				.addPackages(Neo4jBasedService.class) //
				.initialize();
	}

	@Test
	void dingeTesten() {

		Neo4jBasedService client = container.select(Neo4jBasedService.class).get();

		assertThat(client).isNotNull();
		assertThat(client.driver).isNotNull();
		assertThat(client.personRepository).isNotNull();
		Person p = client.personRepository.save(new Person("Hello"));
		assertThat(p.getId()).isNotNull();
	}

}
