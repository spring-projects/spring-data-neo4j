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

package org.springframework.data.graph.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.graph.core.Direction;
import org.springframework.data.graph.core.RelationshipBacked;

/**
 * Annotation for {@link org.springframework.data.graph.annotation.NodeEntity} fields that relate to other entities via
 * relationships. The fields represent read only iterators that provide the relationship-entities
 * {@link org.springframework.persistence.RelatedEntity} of the relationships. The iterator reflects the underlying relationships.
 *
 * <pre>
 * &#64;RelatedToVia([type=&quot;roles&quot;], elementClass=Role.class)
 * Iterator&lt;Role&gt; roles;
 * </pre>
 * @author Michael Hunger
 * @since 27.08.2010
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RelatedToVia {
    /**
     * @return name of the relationship type, optional, can be inferred from the field name
     */
    String type();

    /**
     * @return direction for the relationship, by default outgoing
     */
    Direction direction() default Direction.OUTGOING;

    /**
     * @return target relationship entity class
     */
    Class<? extends RelationshipBacked> elementClass();
}
