/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Field that provides access to an iterator which is created by applying the traversal that is built by the supplied
 * traversal builder to the current node. The result elements are automatically converted to appropriate element
 * entity class instances.
 * <pre>
 * &#64;GraphTraversal(traversalBuilder=FriendTraversalBuilder.class, elementClass=Person.class)
 * Iterable&lt;Person&gt; friends;
 * </pre>
 * @author Michael Hunger
 * @since 15.09.2010
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD,ElementType.METHOD})
public @interface Query {
    /**
     * @return Query to be executed {self} will be provided by the node-id of the current entity other parameters (e.g. {name}) by the given named params
     */
    String value() default "";

    /**
     * @return target type to convert the single result column (if any) to.
     */
    Class<?> elementClass() default Object.class;

    /**
     * @return tuple list of parameters that are replaced in the to the @see query-string {"name", value}
     */
    String[] params() default {};

    // FQN is a fix for javac compiler bug
    org.springframework.data.neo4j.annotation.QueryType type() default org.springframework.data.neo4j.annotation.QueryType.Cypher;
}
