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
package org.springframework.data.neo4j.core.mapping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;
import org.springframework.data.neo4j.core.schema.TargetNode;
import org.springframework.data.neo4j.integration.movies.shared.Movie;

/**
 * @author Michael J. Simons
 */
class PropertyTraverserTest {

	private final Neo4jMappingContext ctx;

	PropertyTraverserTest() {
		this.ctx = new Neo4jMappingContext();
		Set<Class<?>> entities = new HashSet<>();
		entities.add(Movie.class);
		this.ctx.setInitialEntitySet(entities);
		this.ctx.afterPropertiesSet();
	}

	@Test
	void shouldTraverseAll() {

		PropertyTraverser traverser = new PropertyTraverser(this.ctx);
		Map<String, Boolean> includedProperties = new TreeMap<>();
		traverser.traverse(Movie.class, (path, property) -> includedProperties.put(path.toString(), property.isAssociation() && !property.isAnnotationPresent(
				TargetNode.class)));

		Map<String, Boolean> expected = new LinkedHashMap<>();
		expected.put("Movie.actors", true);
		expected.put("Movie.actors.id", false);
		expected.put("Movie.actors.person", false);
		expected.put("Movie.actors.roles", false);
		expected.put("Movie.description", false);
		expected.put("Movie.directors", true);
		expected.put("Movie.directors.actedIn", true);
		expected.put("Movie.directors.actedIn.actors", true);
		expected.put("Movie.directors.actedIn.actors.id", false);
		expected.put("Movie.directors.actedIn.actors.person", false);
		expected.put("Movie.directors.actedIn.actors.roles", false);
		expected.put("Movie.directors.actedIn.description", false);
		expected.put("Movie.directors.actedIn.directors", true);
		expected.put("Movie.directors.actedIn.directors.actedIn", true);
		expected.put("Movie.directors.actedIn.directors.born", false);
		expected.put("Movie.directors.actedIn.directors.id", false);
		expected.put("Movie.directors.actedIn.directors.name", false);
		expected.put("Movie.directors.actedIn.directors.reviewed", true);
		expected.put("Movie.directors.actedIn.released", false);
		expected.put("Movie.directors.actedIn.sequel", true);
		expected.put("Movie.directors.actedIn.sequel.actors", true);
		expected.put("Movie.directors.actedIn.sequel.actors.id", false);
		expected.put("Movie.directors.actedIn.sequel.actors.person", false);
		expected.put("Movie.directors.actedIn.sequel.actors.roles", false);
		expected.put("Movie.directors.actedIn.sequel.description", false);
		expected.put("Movie.directors.actedIn.sequel.directors", true);
		expected.put("Movie.directors.actedIn.sequel.directors.actedIn", true);
		expected.put("Movie.directors.actedIn.sequel.directors.born", false);
		expected.put("Movie.directors.actedIn.sequel.directors.id", false);
		expected.put("Movie.directors.actedIn.sequel.directors.name", false);
		expected.put("Movie.directors.actedIn.sequel.directors.reviewed", true);
		expected.put("Movie.directors.actedIn.sequel.released", false);
		expected.put("Movie.directors.actedIn.sequel.sequel", true);
		expected.put("Movie.directors.actedIn.sequel.sequel.actors", true);
		expected.put("Movie.directors.actedIn.sequel.sequel.description", false);
		expected.put("Movie.directors.actedIn.sequel.sequel.directors", true);
		expected.put("Movie.directors.actedIn.sequel.sequel.released", false);
		expected.put("Movie.directors.actedIn.sequel.sequel.sequel", true);
		expected.put("Movie.directors.actedIn.sequel.sequel.title", false);
		expected.put("Movie.directors.actedIn.sequel.title", false);
		expected.put("Movie.directors.actedIn.title", false);
		expected.put("Movie.directors.born", false);
		expected.put("Movie.directors.id", false);
		expected.put("Movie.directors.name", false);
		expected.put("Movie.directors.reviewed", true);
		expected.put("Movie.directors.reviewed.actors", true);
		expected.put("Movie.directors.reviewed.actors.id", false);
		expected.put("Movie.directors.reviewed.actors.person", false);
		expected.put("Movie.directors.reviewed.actors.roles", false);
		expected.put("Movie.directors.reviewed.description", false);
		expected.put("Movie.directors.reviewed.directors", true);
		expected.put("Movie.directors.reviewed.directors.actedIn", true);
		expected.put("Movie.directors.reviewed.directors.born", false);
		expected.put("Movie.directors.reviewed.directors.id", false);
		expected.put("Movie.directors.reviewed.directors.name", false);
		expected.put("Movie.directors.reviewed.directors.reviewed", true);
		expected.put("Movie.directors.reviewed.released", false);
		expected.put("Movie.directors.reviewed.sequel", true);
		expected.put("Movie.directors.reviewed.sequel.actors", true);
		expected.put("Movie.directors.reviewed.sequel.description", false);
		expected.put("Movie.directors.reviewed.sequel.directors", true);
		expected.put("Movie.directors.reviewed.sequel.released", false);
		expected.put("Movie.directors.reviewed.sequel.sequel", true);
		expected.put("Movie.directors.reviewed.sequel.title", false);
		expected.put("Movie.directors.reviewed.title", false);
		expected.put("Movie.released", false);
		expected.put("Movie.sequel", true);
		expected.put("Movie.sequel.actors", true);
		expected.put("Movie.sequel.description", false);
		expected.put("Movie.sequel.directors", true);
		expected.put("Movie.sequel.released", false);
		expected.put("Movie.sequel.sequel", true);
		expected.put("Movie.sequel.title", false);
		expected.put("Movie.title", false);

		assertThat(includedProperties).containsExactlyEntriesOf(expected);
	}

	@Test
	void onlyMovieDirectFields() {

		PropertyTraverser traverser = new PropertyTraverser(this.ctx);
		Map<String, Boolean> includedProperties = new TreeMap<>();
		traverser.traverse(Movie.class,
				(path, property) -> !property.isAssociation(),
				(path, property) -> includedProperties.put(path.toString(), property.isAssociation()));

		Map<String, Boolean> expected = new LinkedHashMap<>();
		expected.put("Movie.description", false);
		expected.put("Movie.released", false);
		expected.put("Movie.title", false);

		assertThat(includedProperties).containsExactlyEntriesOf(expected);
	}

	@Test
	void onlyDirectors() {

		PropertyTraverser traverser = new PropertyTraverser(this.ctx);
		Map<String, Boolean> includedProperties = new TreeMap<>();
		traverser.traverse(Movie.class,
				(path, property) -> property.getName().equals("directors") || (path.toDotPath().startsWith("directors.")
																			   && property.getName().equals("name")),
				(path, property) -> includedProperties.put(path.toString(), property.isAssociation()));

		Map<String, Boolean> expected = new LinkedHashMap<>();
		expected.put("Movie.directors", true);
		expected.put("Movie.directors.name", false);

		assertThat(includedProperties).containsExactlyEntriesOf(expected);
	}
}
