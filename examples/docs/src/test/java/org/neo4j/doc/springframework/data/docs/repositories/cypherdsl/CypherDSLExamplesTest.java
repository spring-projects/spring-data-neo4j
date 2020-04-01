package org.neo4j.doc.springframework.data.docs.repositories.cypherdsl;

// tag::cypher-dsl-imports[]
import static org.assertj.core.api.Assertions.*;
import static org.neo4j.springframework.data.core.cypher.Conditions.*;
import static org.neo4j.springframework.data.core.cypher.Cypher.*;
import static org.neo4j.springframework.data.core.cypher.Functions.*;

// end::cypher-dsl-imports[]

import org.junit.jupiter.api.Test;
// tag::cypher-dsl-imports[]
import org.neo4j.springframework.data.core.cypher.Cypher;
import org.neo4j.springframework.data.core.cypher.renderer.Renderer;
// end::cypher-dsl-imports[]

/**
 * Examples for the Cypher DSL.
 *
 * @author Michael J. Simons
 * @soundtrack Rage - Lingua Mortis
 * @since 1.0
 */
class CypherDSLExamplesTest {

	private static final Renderer cypherRenderer = Renderer.getDefaultRenderer();

	@Test
	void findAllMovies() {

		// tag::cypher-dsl-e1[]
		var m = node("Movie").named("m"); // <.>
		var statement = Cypher.match(m) // <.>
			.returning(m)
			.build(); // <.>

		assertThat(cypherRenderer.render(statement))
			.isEqualTo("MATCH (m:`Movie`) RETURN m");
		// end::cypher-dsl-e1[]
	}

	@Test
	void playMovieGraphFind() {

		// tag::cypher-dsl-e2[]
		var tom = anyNode().named("tom").properties("name", literalOf("Tom Hanks"));
		var statement = Cypher
			.match(tom).returning(tom)
			.build();

		assertThat(cypherRenderer.render(statement))
			.isEqualTo("MATCH (tom {name: 'Tom Hanks'}) RETURN tom");
		// end::cypher-dsl-e2[]

		// tag::cypher-dsl-e3[]
		var cloudAtlas = anyNode().named("cloudAtlas").properties("title", literalOf("Cloud Atlas"));
		statement = Cypher
			.match(cloudAtlas).returning(cloudAtlas)
			.build();

		assertThat(cypherRenderer.render(statement))
			.isEqualTo("MATCH (cloudAtlas {title: 'Cloud Atlas'}) RETURN cloudAtlas");
		// end::cypher-dsl-e3[]

		// tag::cypher-dsl-e4[]
		var people = node("Person").named("people");
		statement = Cypher
			.match(people)
			.returning(people.property("name"))
			.limit(10)
			.build();

		assertThat(cypherRenderer.render(statement))
			.isEqualTo("MATCH (people:`Person`) RETURN people.name LIMIT 10");
		// end::cypher-dsl-e4[]

		// tag::cypher-dsl-e5[]
		var nineties = node("Movie").named("nineties");
		var released = nineties.property("released");
		statement = Cypher
			.match(nineties)
			.where(released.gte(literalOf(1990)).and(released.lt(literalOf(2000))))
			.returning(nineties.property("title"))
			.build();

		assertThat(cypherRenderer.render(statement))
			.isEqualTo(
				"MATCH (nineties:`Movie`) WHERE (nineties.released >= 1990 AND nineties.released < 2000) RETURN nineties.title");
		// end::cypher-dsl-e5[]
	}

	@Test
	void playMovieGraphQuery() {

		// tag::cypher-dsl-e6[]
		var tom = node("Person").named("tom").properties("name", literalOf("Tom Hanks"));
		var tomHanksMovies = anyNode("tomHanksMovies");
		var statement = Cypher
			.match(tom.relationshipTo(tomHanksMovies, "ACTED_IN"))
			.returning(tom, tomHanksMovies)
			.build();

		assertThat(cypherRenderer.render(statement))
			.isEqualTo(
				"MATCH (tom:`Person` {name: 'Tom Hanks'})-[:`ACTED_IN`]->(tomHanksMovies) RETURN tom, tomHanksMovies");
		// end::cypher-dsl-e6[]

		// tag::cypher-dsl-e7[]
		var cloudAtlas = anyNode("cloudAtlas").properties("title", literalOf("Cloud Atlas"));
		var directors = anyNode("directors");
		statement = Cypher
			.match(cloudAtlas.relationshipFrom(directors, "DIRECTED"))
			.returning(directors.property("name"))
			.build();

		assertThat(cypherRenderer.render(statement))
			.isEqualTo("MATCH (cloudAtlas {title: 'Cloud Atlas'})<-[:`DIRECTED`]-(directors) RETURN directors.name");
		// end::cypher-dsl-e7[]

		// tag::cypher-dsl-e8[]
		tom = node("Person").named("tom").properties("name", literalOf("Tom Hanks"));
		var movie = anyNode("m");
		var coActors = anyNode("coActors");
		var people = node("Person").named("people");
		statement = Cypher
			.match(tom.relationshipTo(movie, "ACTED_IN").relationshipFrom(coActors, "ACTED_IN"))
			.returning(coActors.property("name"))
			.build();

		assertThat(cypherRenderer.render(statement))
			.isEqualTo(
				"MATCH (tom:`Person` {name: 'Tom Hanks'})-[:`ACTED_IN`]->(m)<-[:`ACTED_IN`]-(coActors) RETURN coActors.name");
		// end::cypher-dsl-e8[]

		// tag::cypher-dsl-e9[]
		cloudAtlas = node("Movie").properties("title", literalOf("Cloud Atlas"));
		people = node("Person").named("people");
		var relatedTo = people.relationshipBetween(cloudAtlas).named("relatedTo");
		statement = Cypher
			.match(relatedTo)
			.returning(people.property("name"), type(relatedTo), relatedTo.getRequiredSymbolicName())
			.build();

		assertThat(cypherRenderer.render(statement))
			.isEqualTo(
				"MATCH (people:`Person`)-[relatedTo]-(:`Movie` {title: 'Cloud Atlas'}) RETURN people.name, type(relatedTo), relatedTo");
		// end::cypher-dsl-e9[]
	}

	@Test
	void playMovieGraphSolve() {

		// tag::cypher-dsl-bacon[]
		var bacon = node("Person").named("bacon").properties("name", literalOf("Kevin Bacon"));
		var hollywood = anyNode("hollywood");
		var statement = Cypher
			.match(bacon.relationshipBetween(hollywood).length(1, 4))
			.returningDistinct(hollywood)
			.build();

		assertThat(cypherRenderer.render(statement))
			.isEqualTo("MATCH (bacon:`Person` {name: 'Kevin Bacon'})-[*1..4]-(hollywood) RETURN DISTINCT hollywood");
		// end::cypher-dsl-bacon[]
	}

	@Test
	void playMovieGraphRecommend() {

		// tag::cypher-dsl-r[]
		var tom = node("Person").named("tom").properties("name", literalOf("Tom Hanks"));
		var coActors = anyNode("coActors");
		var cocoActors = anyNode("cocoActors");
		var strength = count(asterisk()).as("Strength");
		var statement = Cypher
			.match(
				tom.relationshipTo(anyNode("m"), "ACTED_IN").relationshipFrom(coActors, "ACTED_IN"),
				coActors.relationshipTo(anyNode("m2"), "ACTED_IN").relationshipFrom(cocoActors, "ACTED_IN")
			)
			.where(not(tom.relationshipTo(anyNode(), "ACTED_IN").relationshipFrom(cocoActors, "ACTED_IN")))
			.and(tom.isNotEqualTo(cocoActors))
			.returning(
				cocoActors.property("name").as("Recommended"),
				strength
			).orderBy(strength.asName().descending())
			.build();

		assertThat(cypherRenderer.render(statement))
			.isEqualTo(""
				+ "MATCH "
				+ "(tom:`Person` {name: 'Tom Hanks'})-[:`ACTED_IN`]->(m)<-[:`ACTED_IN`]-(coActors), "
				+ "(coActors)-[:`ACTED_IN`]->(m2)<-[:`ACTED_IN`]-(cocoActors) "
				+ "WHERE (NOT (tom)-[:`ACTED_IN`]->()<-[:`ACTED_IN`]-(cocoActors) AND tom <> cocoActors) "
				+ "RETURN cocoActors.name AS Recommended, count(*) AS Strength ORDER BY Strength DESC");
		// end::cypher-dsl-r[]
	}
}
