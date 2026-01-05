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
package org.springframework.data.neo4j.core

import org.neo4j.cypherdsl.core.Statement

/**
 * Extension for [Neo4jOperations.count] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
inline fun <reified T : Any> Neo4jOperations.count(): Long = count(T::class.java)

/**
 * Extension for [Neo4jOperations.findAll] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
inline fun <reified T : Any> Neo4jOperations.findAll(): List<T> = findAll(T::class.java)

/**
 * Extension for [Neo4jOperations.findAll] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
inline fun <reified T : Any> Neo4jOperations.findAll(statement: Statement): List<T> = findAll(statement, T::class.java)

/**
 * Extension for [Neo4jOperations.findAll] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
inline fun <reified T : Any> Neo4jOperations.findAll(statement: Statement, parameters: Map<String, Any>): List<T> = findAll(statement, parameters, T::class.java)

/**
 * Extension for [Neo4jOperations.findOne] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
inline fun <reified T : Any> Neo4jOperations.findOne(statement: Statement, parameters: Map<String, Any>): T? = findOne(statement, parameters, T::class.java).orElse(null)

/**
 * Extension for [Neo4jOperations.findById] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
inline fun <reified T : Any> Neo4jOperations.findById(id: Any): T? = findById(id, T::class.java).orElse(null)

/**
 * Extension for [Neo4jOperations.findAllById] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
inline fun <reified T : Any> Neo4jOperations.findAllById(ids: Iterable<Any>): List<T> = findAllById(ids, T::class.java)

/**
 * Extension for [Neo4jOperations.deleteById] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
inline fun <reified T : Any> Neo4jOperations.deleteById(id: Any): Unit = deleteById(id, T::class.java)

/**
 * Extension for [Neo4jOperations.deleteAllById] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
inline fun <reified T : Any> Neo4jOperations.deleteAllById(ids: Iterable<Any>): Unit = deleteAllById(ids, T::class.java)

/**
 * Extension for [Neo4jOperations.deleteAll] leveraging reified type parameters.
 * @author Michael J. Simons
 * @since 6.1
 */
inline fun <reified T : Any> Neo4jOperations.deleteAll(): Unit = deleteAll(T::class.java)
