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
package org.neo4j.springframework.data.integration.imperative

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.neo4j.driver.Driver
import org.neo4j.driver.Values
import org.neo4j.springframework.data.config.AbstractNeo4jConfig
import org.neo4j.springframework.data.core.Neo4jClient
import org.neo4j.springframework.data.core.fetchAs
import org.neo4j.springframework.data.core.mappedBy
import org.neo4j.springframework.data.test.Neo4jExtension
import org.neo4j.springframework.data.test.Neo4jIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.annotation.EnableTransactionManagement

/**
 * Integration tests for using the Neo4j client in a Kotlin program.
 *
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class Neo4jClientKotlinInteropIT @Autowired constructor(
    private val driver: Driver,
    private val neo4jClient: Neo4jClient
) {

    companion object {
        @JvmStatic
        private lateinit var neo4jConnectionSupport: Neo4jExtension.Neo4jConnectionSupport
    }

    @BeforeEach
    fun prepareData() {

        driver.session().use {
            val bands = mapOf(
                    Pair("Queen", listOf("Brian", "Roger", "John", "Freddie")),
                    Pair("Die Ärzte", listOf("Farin", "Rod", "Bela"))
            )

            bands.forEach { b, m ->
                val summary = it.run("""
                    CREATE (b:Band {name: ${'$'}band}) 
                    WITH b
                    UNWIND ${'$'}names AS name CREATE (n:Member {name: name}) <- [:HAS_MEMBER] - (b)
                    """.trimIndent(), Values.parameters("band", b, "names", m)).consume()
                assertThat(summary.counters().nodesCreated()).isGreaterThan(0)
            }
        }
    }

    @AfterEach
    fun purgeData() {

        driver.session().use { it.run("MATCH (n) DETACH DELETE n").consume() }
    }

    data class Artist(val name: String)

    data class Band(val name: String, val member: Collection<Artist>)

    @Test
    fun `The Neo4j client should be usable from idiomatic Kotlin code`() {

        val dieAerzte = neo4jClient
                .query(" MATCH (b:Band {name: \$name}) - [:HAS_MEMBER] -> (m)" +
                    " RETURN b as band, collect(m.name) as members")
                .bind("Die Ärzte").to("name")
                .mappedBy { _, r ->
                    val members = r["members"].asList { v -> Artist(v.asString()) }
                    Band(r["band"]["name"].asString(), members)
                }
                .one()

        assertThat(dieAerzte).isNotNull
        assertThat(dieAerzte!!.member).hasSize(3)

        if (neo4jClient.query("MATCH (n:IDontExists) RETURN id(n)").fetchAs<Long>().one() != null) {
            Assertions.fail<String>("The record does not exist, the optional had to be null")
        }
    }

    @Configuration
    @EnableTransactionManagement
    open class Config : AbstractNeo4jConfig() {

        @Bean
        override fun driver(): Driver {
            return neo4jConnectionSupport.driver
        }
    }
}
