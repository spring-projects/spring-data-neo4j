/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.data.graph.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to declare an Pojo-Entity as graph backed. It is used by the {@link org.springframework.data.graph.neo4j.support.node.Neo4jNodeBacking} aspect to
 * introduce constructor and field advices as well as add the {@link org.springframework.data.graph.core.NodeBacked} interface.
 * @author Michael Hunger
 * @since 27.08.2010
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NodeEntity {
    /**
     * @return true if the property names default to field names, otherwise the FQN of the class will be prepended
     */
    boolean useShortNames() default true;

    /**
     * @return true if all fields of the entity will be indexed by default
     */
    boolean fullIndex() default false;

    /**
     * @return true if the entity is only partially managed by the {@link org.springframework.data.graph.neo4j.support.node.Neo4jNodeBacking} aspect
     * If partial is set, then construction of the node is delayed until the entity's id has been set by another persistent store. And only annotated fields
     * will be handled by the graph storage.
     */
    boolean partial() default false;
}
