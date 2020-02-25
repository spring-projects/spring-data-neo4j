package org.neo4j.doc.springframework.data.docs.repositories.populators;

// tag::populators[]
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.repository.init.Jackson2RepositoryPopulatorFactoryBean;
import org.springframework.data.repository.init.ResourceReaderRepositoryPopulator;

import com.fasterxml.jackson.databind.ObjectMapper;

// end::populators[]

/**
 * An example how to configure a repository populator.
 *
 * @author Michael J. Simons
 * @soundtrack Rammstein - Reise Reise
 */
// tag::populators[]
@Configuration
public class PopulatorConfig {

	@Bean
	public FactoryBean<ResourceReaderRepositoryPopulator> respositoryPopulator(
		ObjectMapper objectMapper, // <1>
		ResourceLoader resourceLoader) {

		Jackson2RepositoryPopulatorFactoryBean factory = new Jackson2RepositoryPopulatorFactoryBean();
		factory.setMapper(objectMapper);
		factory.setResources(new Resource[] { resourceLoader.getResource("classpath:data.json") }); // <2>
		return factory;
	}
}
// end::populators[]
