/*
 * Copyright (c) 2023-2024 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
