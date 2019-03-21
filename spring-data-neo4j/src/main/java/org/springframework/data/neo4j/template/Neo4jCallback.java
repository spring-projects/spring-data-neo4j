/*
 * Copyright 2011-2019 the original author or authors.
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
package org.springframework.data.neo4j.template;

import org.neo4j.ogm.session.Session;

/**
 * Callback interface for Neo4j OGM code. To be used with {@link Neo4jTemplate}'s execution methods, often as anonymous
 * classes within a method implementation. A typical implementation will call {@code Session.load/find/update} to
 * perform some operations on persistent objects.
 *
 * @author Juergen Hoeller
 * @see Neo4jTemplate
 * @see org.springframework.data.neo4j.transaction.Neo4jTransactionManager
 * @since 4.2
 */
@FunctionalInterface
public interface Neo4jCallback<T> {

	/**
	 * Gets called by {@code Neo4jTemplate.doExecute} with an active OGM {@code Session}. Does not need to care about
	 * activating or closing the {@code Session}, or handling transactions.
	 * <p>
	 * Allows for returning a result object created within the callback, i.e. a domain object or a collection of domain
	 * objects. A thrown custom RuntimeException is treated as an application exception: It gets propagated to the caller
	 * of the template.
	 *
	 * @param session active Neo4j session
	 * @return a result object, or {@code null} if none
	 */
	T doInNeo4jOgm(Session session);
}
