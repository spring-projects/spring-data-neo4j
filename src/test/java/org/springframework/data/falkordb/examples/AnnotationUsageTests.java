/*
 * Copyright (c) 2023-2024 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.springframework.data.falkordb.examples;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.springframework.data.falkordb.repository.query.Query;
import org.springframework.data.falkordb.core.schema.TargetNode;
import org.springframework.data.falkordb.core.schema.RelationshipId;
import org.springframework.data.falkordb.core.schema.RelationshipProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class demonstrating the usage of {@link Query} and {@link TargetNode} annotations.
 *
 * <p>
 * This class showcases:
 * <ul>
 * <li>How to use {@link Query} annotation for custom Cypher queries</li>
 * <li>How to use {@link TargetNode} annotation in relationship properties</li>
 * <li>Various parameter binding techniques</li>
 * <li>Different query types (count, exists, write operations)</li>
 * </ul>
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
class AnnotationUsageTests {

	/**
	 * Test demonstrating the structure of entities using @TargetNode annotation. This
	 * test doesn't run actual queries but shows the annotation usage patterns.
	 */
	@Test
	void demonstrateTargetNodeAnnotation() {
		// Given: A person entity
		Person keanuReeves = new Person();
		keanuReeves.setName("Keanu Reeves");
		keanuReeves.setAge(60); // Approximate current age

		// When: Creating a relationship with properties using @TargetNode
		ActedIn actedInMatrix = new ActedIn(keanuReeves, Arrays.asList("Neo", "Mr. Anderson"), 1999);

		// Then: The relationship properly links to the target node
		assertThat(actedInMatrix.getActor()).isEqualTo(keanuReeves);
		assertThat(actedInMatrix.getRoles()).containsExactly("Neo", "Mr. Anderson");
		assertThat(actedInMatrix.getYear()).isEqualTo(1999);
	}

	/**
	 * Test demonstrating the structure of a movie with relationship properties.
	 */
	@Test
	void demonstrateMovieWithRelationshipProperties() {
		// Given: A movie with actors through relationship properties
		Movie matrix = new Movie("The Matrix", "Welcome to the Real World", 1999);

		Person keanuReeves = new Person();
		keanuReeves.setName("Keanu Reeves");

		Person laurenceFishburne = new Person();
		laurenceFishburne.setName("Laurence Fishburne");

		// Creating relationship entities with properties
		ActedIn keanuActing = new ActedIn(keanuReeves, Arrays.asList("Neo"), 1999);
		ActedIn laurenceActing = new ActedIn(laurenceFishburne, Arrays.asList("Morpheus"), 1999);

		matrix.setActors(Arrays.asList(keanuActing, laurenceActing));

		// Then: The movie properly contains relationship properties
		assertThat(matrix.getActors()).hasSize(2);
		assertThat(matrix.getActors().get(0).getActor().getName()).isEqualTo("Keanu Reeves");
		assertThat(matrix.getActors().get(1).getActor().getName()).isEqualTo("Laurence Fishburne");
	}

	/**
	 * This test demonstrates the various ways @Query annotation can be used. Note: This
	 * is a documentation test showing the repository method signatures.
	 */
	@Test
	void demonstrateQueryAnnotationUsage() {
		// The MovieRepository interface demonstrates various @Query usage patterns:

		// 1. Simple parameter binding by name:
		// @Query("MATCH (m:Movie) WHERE m.released > $year RETURN m")
		// List<Movie> findMoviesReleasedAfter(@Param("year") Integer year);

		// 2. Parameter binding by index:
		// @Query("MATCH (m:Movie) WHERE m.title CONTAINS $0 RETURN m")
		// List<Movie> findMoviesByTitleContaining(String titlePart);

		// 3. Complex query with relationships:
		// @Query("MATCH (m:Movie {title: $title})-[r:ACTED_IN]-(p:Person) RETURN m,
		// collect(r), collect(p)")
		// Optional<Movie> findMovieWithActors(@Param("title") String title);

		// 4. Entity parameter with __id__ syntax:
		// @Query("MATCH (m:Movie {title: $movie.__id__})-[:ACTED_IN]-(p:Person) RETURN
		// p")
		// List<Person> findActorsInMovie(@Param("movie") Movie movie);

		// 5. Count query:
		// @Query(value = "MATCH (m:Movie) WHERE m.released > $year RETURN count(m)",
		// count = true)
		// Long countMoviesReleasedAfter(@Param("year") Integer year);

		// 6. Exists query:
		// @Query(value = "MATCH (m:Movie {title: $title}) RETURN count(m) > 0", exists =
		// true)
		// Boolean existsByTitle(@Param("title") String title);

		// 7. Write operation:
		// @Query(value = "MATCH (m:Movie {title: $title}) SET m.updated = timestamp()
		// RETURN m", write = true)
		// Movie updateMovieTimestamp(@Param("title") String title);

		assertThat(true).isTrue(); // This test is for documentation purposes
	}

	/**
	 * Demonstrates the relationship between @RelationshipProperties, @TargetNode,
	 * and @RelationshipId.
	 */
	@Test
	void demonstrateRelationshipAnnotationsInteraction() {
		// Example relationship properties class structure:

		// @RelationshipProperties
		// public class ActedIn {
		// @RelationshipId // Marks the relationship's internal ID
		// private Long id;
		//
		// @TargetNode // Marks the target node of the relationship
		// private Person actor;
		//
		// private List<String> roles; // Relationship properties
		// private Integer year; // Relationship properties
		// }

		// This structure allows Spring Data FalkorDB to:
		// 1. Identify the relationship ID field
		// 2. Know which field represents the target node
		// 3. Map additional properties on the relationship edge

		assertThat(true).isTrue(); // This test is for documentation purposes
	}

}
