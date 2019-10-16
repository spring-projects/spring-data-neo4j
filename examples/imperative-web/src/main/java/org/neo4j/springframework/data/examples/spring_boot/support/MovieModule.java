/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.springframework.data.examples.spring_boot.support;

import org.neo4j.springframework.data.examples.spring_boot.domain.MovieEntity;
import org.neo4j.springframework.data.examples.spring_boot.domain.PersonEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.module.SimpleModule;

/**
 * A jackson module supporting our domain package.
 *
 * @author Michael J. Simons
 * @soundtrack Rammstein - Reise Reise
 */
@Component
public class MovieModule extends SimpleModule {

	public MovieModule() {
		setMixInAnnotation(MovieEntity.class, MovieEntityMixin.class);
		setMixInAnnotation(PersonEntity.class, PersonEntityMixin.class);
	}

	abstract static class MovieEntityMixin {
		@JsonCreator MovieEntityMixin(@JsonProperty("title") final String title,
			@JsonProperty("description") final String description) {
		}
	}

	abstract static class PersonEntityMixin {
		@JsonCreator PersonEntityMixin(@JsonProperty("name") final String name, @JsonProperty("born") final Long born) {
		}
	}
}
