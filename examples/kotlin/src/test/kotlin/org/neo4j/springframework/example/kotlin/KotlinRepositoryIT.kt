package org.neo4j.springframework.example.kotlin

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.neo4j.driver.Driver
import org.neo4j.springframework.boot.test.autoconfigure.data.ReactiveDataNeo4jTest
import org.neo4j.springframework.example.kotlin.domain.MovieRepository
import org.neo4j.springframework.example.kotlin.domain.PersonRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.Neo4jContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.test.StepVerifier
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors

@Testcontainers
@ReactiveDataNeo4jTest
class KotlinRepositoryIT {

    @BeforeEach
    fun setup(@Autowired driver: Driver?) {
        BufferedReader(
            InputStreamReader(this.javaClass.getResourceAsStream("/movies.cypher"))).use { moviesReader ->
            driver!!.session().use { session ->
                session.run("MATCH (n) DETACH DELETE n")

                val moviesCypher = moviesReader.readText()
                // consume all results from the driver
                session.run(moviesCypher).consume()
            }
        }
    }

    @Test
    fun loadAllPersonsFromGraph(@Autowired personRepository: PersonRepository) {
        val expectedPersonCount = 133
        StepVerifier.create(personRepository.findAll())
            .expectNextCount(expectedPersonCount.toLong())
            .verifyComplete()
    }

    @Test
    fun findPersonByName(@Autowired personRepository: PersonRepository) {
        StepVerifier.create(personRepository.findByName("Tom Hanks"))
            .assertNext { personEntity -> assertThat(personEntity.born).isEqualTo(1956) }
            .verifyComplete()
    }

    @Test
    fun findsPersonsWhoActAndDirect(@Autowired personRepository: PersonRepository) {
        val expectedActorAndDirectorCount = 5
        StepVerifier.create(personRepository.getPersonsWhoActAndDirect())
            .expectNextCount(expectedActorAndDirectorCount.toLong())
            .verifyComplete()
    }

    @Test
    fun findOneMovie(@Autowired movieRepository: MovieRepository) {
        StepVerifier.create(movieRepository.findOneByTitle("The Matrix"))
            .assertNext { movie ->
                assertThat(movie.title).isEqualTo("The Matrix")
                assertThat(movie.description).isEqualTo("Welcome to the Real World")
                assertThat(movie.directors).hasSize(2)
                assertThat(movie.actorsAndRoles).hasSize(5)
            }
            .verifyComplete()
    }

    companion object {
        @Container
        @JvmStatic
        private val neo4jContainer = Neo4jContainer<Nothing>("neo4j:4.0")

        @DynamicPropertySource
        @JvmStatic
        fun neo4jProperties(registry: DynamicPropertyRegistry?) {
            registry!!.add("org.neo4j.driver.uri") { neo4jContainer.boltUrl }
            registry.add("org.neo4j.driver.authentication.username") { "neo4j" }
            registry.add("org.neo4j.driver.authentication.password") { neo4jContainer.adminPassword }
        }
    }
}
