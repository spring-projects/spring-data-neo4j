/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.falkordb.repository.query;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;
import org.springframework.core.annotation.AliasFor;

/**
 * Annotation to provide a custom Cypher query for repository query methods.
 * <p>
 * This annotation can be used to define custom Cypher queries that cannot be expressed as
 * derived queries. The query can contain parameters that are bound to method arguments
 * using the {@code $parameter} syntax or
 * {@link org.springframework.data.repository.query.Param @Param} annotation.
 * <p>
 * Example usage: <pre>
 * public interface UserRepository extends FalkorDBRepository&lt;User, Long&gt; {
 *
 *     &#64;Query("MATCH (u:User)-[:FOLLOWS]->(f:User) WHERE u.username = $username RETURN f")
 *     List&lt;User&gt; findFollowing(&#64;Param("username") String username);
 *
 *     &#64;Query("MATCH (u:User) WHERE u.age > $0 RETURN u")
 *     List&lt;User&gt; findUsersOlderThan(int age);
 *
 *     &#64;Query("MATCH (u:User {id: $user.__id__})-[:FOLLOWS]->(f) RETURN u, collect(f)")
 *     User findUserWithFollowing(&#64;Param("user") User user);
 * }
 * </pre>
 * <p>
 * Parameters can be bound in several ways:
 * <ul>
 * <li>By parameter index: {@code $0}, {@code $1}, etc.</li>
 * <li>By parameter name using {@code @Param}: {@code $paramName}</li>
 * <li>By object property: {@code $object.property} or {@code $object.__id__} for entity
 * IDs</li>
 * </ul>
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 * @see org.springframework.data.repository.query.Param
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
@API(status = API.Status.STABLE, since = "1.0")
public @interface Query {

	/**
	 * Returns the Cypher query to be executed.
	 * @return See {@link #value()}.
	 */
	@AliasFor("cypher")
	String value() default "";

	/**
	 * Returns the Cypher query to be executed.
	 * @return The Cypher query string.
	 */
	@AliasFor("value")
	String cypher() default "";

	/**
	 * Defines whether the given query should be executed as count projection.
	 * <p>
	 * When set to {@literal true}, the query is expected to project a single numeric
	 * value that will be returned as the result. This is useful for count queries.
	 * @return {@literal true} if the query is a count projection, {@literal false}
	 * otherwise.
	 */
	boolean count() default false;

	/**
	 * Defines whether the given query should be executed as exists projection.
	 * <p>
	 * When set to {@literal true}, the query is expected to return a boolean value
	 * indicating the existence of a particular condition.
	 * @return {@literal true} if the query is an exists projection, {@literal false}
	 * otherwise.
	 */
	boolean exists() default false;

	/**
	 * Defines whether the query should be executed as a write operation.
	 * <p>
	 * Set this to {@literal true} for queries that modify data (CREATE, UPDATE, DELETE,
	 * etc.). This affects how the query is executed and how transactions are handled.
	 * @return {@literal true} if this is a write operation, {@literal false} for
	 * read-only queries.
	 */
	boolean write() default false;

}