/*
 * Copyright 2011-2021 the original author or authors.
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

package org.springframework.data.neo4j.integration.imperative

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.neo4j.driver.Driver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.neo4j.config.AbstractNeo4jConfig
import org.springframework.data.neo4j.core.DatabaseSelectionProvider
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager
import org.springframework.data.neo4j.integration.shared.common.DeviceEntity
import org.springframework.data.neo4j.integration.shared.common.ImmutableKotlinPerson
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories
import org.springframework.data.neo4j.test.BookmarkCapture
import org.springframework.data.neo4j.test.Neo4jExtension
import org.springframework.data.neo4j.test.Neo4jIntegrationTest
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.Transactional

/**
 * This test originate from https://github.com/neo4j/SDN/issues/102.
 * It is designed to ensure the capability of creating dependent relationships for immutable objects before
 * the creation of the object itself.
 *
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
@Neo4jIntegrationTest
class ImmutableRelationshipsIT @Autowired constructor(
        private val repository: DeviceRepository,
        private val personRepository: ImmutableKotlinPersonRepository,
        private val driver: Driver,
        private val bookmarkCapture: BookmarkCapture
) {

    companion object {
        @JvmStatic
        private lateinit var neo4jConnectionSupport: Neo4jExtension.Neo4jConnectionSupport
    }

    @Test
    fun createRelationshipsBeforeRootObject() {

        driver.session(bookmarkCapture.createSessionConfig()).use { session ->
            session.run("MATCH (n) DETACH DELETE n").consume()
            session.run("CREATE (n:DeviceEntity {deviceId:'123', phoneNumber:'some number'})-[:LATEST_LOCATION]->(l1: LocationEntity{latitude: 20.0, longitude: 20.0})").consume()
            bookmarkCapture.seedWith(session.lastBookmark())
        }

        val device = repository.findById("123").get()
        assertThat(device.deviceId).isEqualTo("123")
        assertThat(device.phoneNumber).isEqualTo("some number")

        assertThat(device.location!!.latitude).isEqualTo(20.0)
        assertThat(device.location.longitude).isEqualTo(20.0)
    }

    @Test
    fun createDeepSameClassRelationshipsBeforeRootObject() {

        driver.session(bookmarkCapture.createSessionConfig()).use { session ->
            session.run("MATCH (n) DETACH DELETE n").consume()
            session.run("CREATE (n:DeviceEntity {deviceId:'123', phoneNumber:'some number'})" +
                    "-[:LATEST_LOCATION]->" +
                    "(l1: LocationEntity{latitude: 10.0, longitude: 20.0})" +
                    "-[:PREVIOUS_LOCATION]->" +
                    "(l2: LocationEntity{latitude: 30.0, longitude: 40.0})").consume()
            bookmarkCapture.seedWith(session.lastBookmark())
        }
        val device = repository.findById("123").get()
        assertThat(device.deviceId).isEqualTo("123")
        assertThat(device.phoneNumber).isEqualTo("some number")

        assertThat(device.location!!.latitude).isEqualTo(10.0)
        assertThat(device.location.longitude).isEqualTo(20.0)
        assertThat(device.location.previousLocation!!.latitude).isEqualTo(30.0)
        assertThat(device.location.previousLocation.longitude).isEqualTo(40.0)
    }

    @Test
    fun createComplexSameClassRelationshipsBeforeRootObject() {

        driver.session(bookmarkCapture.createSessionConfig()).use { session ->
            session.run("MATCH (n) DETACH DELETE n").consume()
            bookmarkCapture.seedWith(session.lastBookmark())
        }

        val p1 = ImmutableKotlinPerson("Person1", emptyList())
        val p2 = ImmutableKotlinPerson("Person2", listOf(p1))
        val p3 = ImmutableKotlinPerson("Person3", listOf(p1, p2))

        personRepository.save(p3)

        val people = personRepository.findAll()

        assertThat(people).hasSize(3)
    }

    @Configuration
    @EnableTransactionManagement
    @EnableNeo4jRepositories
    open class MyConfig : AbstractNeo4jConfig() {
        @Bean
        override fun driver(): Driver {
            return neo4jConnectionSupport.driver
        }

        @Bean
        open fun bookmarkCapture(): BookmarkCapture {
            return BookmarkCapture()
        }

        @Bean
        override fun transactionManager(driver: Driver, databaseNameProvider: DatabaseSelectionProvider): PlatformTransactionManager {
            val bookmarkCapture = bookmarkCapture()
            return Neo4jTransactionManager(driver, databaseNameProvider, Neo4jBookmarkManager.create(bookmarkCapture))
        }
    }

}

interface DeviceRepository : Neo4jRepository<DeviceEntity, String>
interface ImmutableKotlinPersonRepository : Neo4jRepository<ImmutableKotlinPerson, String>
