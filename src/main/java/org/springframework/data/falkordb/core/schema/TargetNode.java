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
package org.springframework.data.falkordb.core.schema;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * Annotation to mark a field in a {@link RelationshipProperties} annotated class as the
 * target node of the relationship.
 * <p>
 * This annotation is used in conjunction with {@link RelationshipProperties} to define
 * relationship entities that have properties. The field annotated with
 * {@code @TargetNode} represents the target node of the relationship from the perspective
 * of the source node.
 * <p>
 * Example usage: <pre>
 * &#64;RelationshipProperties
 * public class ActedIn {
 *
 *     &#64;RelationshipId
 *     private Long id;
 *
 *     &#64;TargetNode
 *     private Person actor;
 *
 *     private List&lt;String&gt; roles;
 *     private Integer year;
 *
 *     // constructors, getters, setters...
 * }
 *
 * &#64;Node
 * public class Movie {
 *
 *     &#64;Id
 *     private String title;
 *
 *     &#64;Relationship(type = "ACTED_IN", direction = Direction.INCOMING)
 *     private List&lt;ActedIn&gt; actors = new ArrayList&lt;&gt;();
 *
 *     // other fields...
 * }
 * </pre>
 * <p>
 * In the above example, the {@code ActedIn} class represents a relationship with
 * properties between a {@code Movie} and a {@code Person}. The {@code actor} field marked
 * with {@code @TargetNode} represents the Person node that is the target of the ACTED_IN
 * relationship.
 * <p>
 * The {@code @TargetNode} annotation helps Spring Data FalkorDB understand the structure
 * of the relationship and properly map the relationship properties and target node when
 * loading and saving entities.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 * @see RelationshipProperties
 * @see Relationship
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@Inherited
@API(status = API.Status.STABLE, since = "1.0")
public @interface TargetNode {

}
