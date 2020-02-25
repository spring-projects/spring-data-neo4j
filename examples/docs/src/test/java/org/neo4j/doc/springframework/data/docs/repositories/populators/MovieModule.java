package org.neo4j.doc.springframework.data.docs.repositories.populators;

// tag::populators[]
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.module.SimpleModule;

// end::populators[]

/**
 * A jackson module supporting our domain package.
 *
 * @author Michael J. Simons
 * @soundtrack Rammstein - Reise Reise
 */
// tag::populators[]
@Component
public class MovieModule extends SimpleModule {

	public MovieModule() {
		setMixInAnnotation(MovieEntity.class, MovieEntityMixin.class);
		// end::populators[]
		setMixInAnnotation(PersonEntity.class, PersonEntityMixin.class);
	}
	// tag::populators[]

	abstract static class MovieEntityMixin {
		@JsonCreator MovieEntityMixin(@JsonProperty("title") final String title,
			@JsonProperty("description") final String description) {
		}
	}

	// end::populators[]
	abstract static class PersonEntityMixin {
		@JsonCreator PersonEntityMixin(@JsonProperty("name") final String name, @JsonProperty("born") final Long born) {
		}
	}

	// tag::populators[]
}
// end::populators[]

