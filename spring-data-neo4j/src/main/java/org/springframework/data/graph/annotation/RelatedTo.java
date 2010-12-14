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

import org.springframework.data.graph.core.Direction;
import org.springframework.data.graph.core.NodeBacked;

/**
 * Annotation for {@link org.springframework.data.graph.annotation.NodeEntity} fields that relate to other entities via
 * relationships. Works for one-to-one and one-to-many relationships. It is optionally possible to define the relationship type,
 * relationship direction and target class (required for one-many-relationships).
 *
 * Collection based one-to-many relationships return managed collections that reflect addition and removal to the underlying relationships.
 *
 * <pre>
 * &#64;RelatedTo([type=&quot;friends&quot;], elementClass=Person.class)
 * Collection&lt;Person&gt; friends;
 * &#64;RelatedTo([type=&quot;spouse&quot;], [elementClass=Person.class])
 * Person spouse;
 * </pre>

 * @author Michael Hunger
 * @since 27.08.2010
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RelatedTo {
    /**
     * @return name of the relationship type, optional, can be inferred from the field name
     */
    String type();

    /**
     * @return direction for the relationship, by default outgoing
     */
    Direction direction() default Direction.OUTGOING;

    /**
     * @return target class, required for collection based fields (no generic inferring)
     */
    Class<? extends NodeBacked> elementClass() default NodeBacked.class;
}
