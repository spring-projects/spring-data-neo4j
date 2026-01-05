/*
 * Copyright 2011-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.documentation.repositories.custom_queries;

import java.util.Collection;

import org.springframework.data.neo4j.documentation.domain.MovieEntity;
import org.springframework.transaction.annotation.Transactional;

interface NonDomainResults {

	@Transactional(readOnly = true)
	Collection<Result> findRelationsToMovie(MovieEntity movie); // <.>

	class Result {

		// <.>
		public final String name;

		public final String typeOfRelation;

		Result(String name, String typeOfRelation) {
			this.name = name;
			this.typeOfRelation = typeOfRelation;
		}

	}

}
