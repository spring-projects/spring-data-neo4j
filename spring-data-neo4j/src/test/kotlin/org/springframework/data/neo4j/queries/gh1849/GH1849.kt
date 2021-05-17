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
package org.springframework.data.neo4j.queries.gh1849

import org.neo4j.ogm.annotation.GeneratedValue
import org.neo4j.ogm.annotation.Id
import org.neo4j.ogm.annotation.Relationship
import org.springframework.data.neo4j.repository.Neo4jRepository
import org.springframework.transaction.annotation.Transactional

class Kennel {
	@Id
	@GeneratedValue
	var id: Long? = null

	@Relationship(type = "HOUSES", direction = Relationship.OUTGOING)
	lateinit var pet: Pet
}

class Person(var name: String) {
	@Id
	@GeneratedValue
	var id: Long? = null

	override fun toString() = "Person(${name})"
}

abstract class Pet(var name: String) {
	@Id
	@GeneratedValue
	var id: Long? = null

	abstract var person: Person?

	override fun toString() = "${this::class.simpleName}(${name})"
}

class Dog(name: String, val breed: String) : Pet(name) {
	@Relationship(type = "OWNED_BY", direction = Relationship.OUTGOING)
	override var person: Person? = null
}

class Cat(name: String, val food: String) : Pet(name) {
	@Relationship(type = "LIVES_WITH", direction = Relationship.OUTGOING)
	override var person: Person? = null
}

@Transactional
interface KennelRepository : Neo4jRepository<Kennel, Long> {

	@Transactional
	override fun findAll(depth: Int): MutableIterable<Kennel>;
}