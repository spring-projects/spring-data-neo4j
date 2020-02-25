package org.neo4j.doc.springframework.data.docs.repositories.populators;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.neo4j.harness.ServerControls;
import org.neo4j.harness.TestServerBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(initializers = PopulatorsTest.TestServerInitializer.class)
public class PopulatorsTest {

	@Autowired
	private MovieRepository movieRepository;

	@Test
	void populatorShouldRun() {

		assertThat(movieRepository.findOneByTitle("All The Way, Boys"))
			.isPresent()
			.get()
			.satisfies(m -> {
				assertThat(m.getActors()).extracting(PersonEntity::getName)
					.containsExactlyInAnyOrder("Bud Spencer", "Terence Hill");
			});
	}

	static class TestServerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
			final ServerControls serverControls = TestServerBuilders.newInProcessBuilder().newServer();
			configurableApplicationContext
				.addApplicationListener((ApplicationListener<ContextClosedEvent>) event -> serverControls.close());

			TestPropertyValues.of(
				"org.neo4j.driver.uri=" + serverControls.boltURI().toString()
			).applyTo(configurableApplicationContext.getEnvironment());
		}
	}
}
