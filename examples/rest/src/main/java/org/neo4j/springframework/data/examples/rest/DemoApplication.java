package org.neo4j.springframework.data.examples.rest;

import org.neo4j.springframework.data.examples.rest.domain.MovieEntity;
import org.neo4j.springframework.data.examples.rest.domain.PersonEntity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.stereotype.Component;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	/**
	 * We explicitly enable the exposure of the identifiers here to get them included in the response
	 * and not only in the links.
	 */
	@Component
	class SpringDataRestCustomization implements RepositoryRestConfigurer {

		@Override
		public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config) {
			config.exposeIdsFor(MovieEntity.class, PersonEntity.class);
		}
	}
}
