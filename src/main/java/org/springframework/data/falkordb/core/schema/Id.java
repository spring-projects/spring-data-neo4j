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
 * This annotation marks an attribute as the primary id of a FalkorDB node entity. It can
 * be used as an alternative to {@link org.springframework.data.annotation.Id} and
 * provides FalkorDB-specific features.
 *
 * <p>
 * To use assigned ids, annotate an arbitrary attribute of your domain class with
 * {@link org.springframework.data.annotation.Id} or this annotation:
 *
 * <pre>
 * &#64;Node
 * public class MyEntity {
 *     &#64;Id String theId;
 * }
 * </pre>
 *
 * You can combine {@code @Id} with {@code @Property} with assigned ids to rename the node
 * property in which the assigned id is stored.
 *
 * <p>
 * To use internally generated ids, annotate an arbitrary attribute of type
 * {@code java.lang.long} or {@code java.lang.Long} with {@code @Id} and
 * {@link GeneratedValue @GeneratedValue}.
 *
 * <pre>
 * &#64;Node
 * public class MyEntity {
 *     &#64;Id &#64;GeneratedValue Long id;
 * }
 * </pre>
 *
 * It does not need to be named {@code id}, but most people chose this as the attribute in
 * the class. As the attribute does not correspond to a node property, it cannot be
 * renamed via {@code @Property}.
 *
 * <p>
 * To use externally generated ids, annotate an arbitrary attribute with a type that your
 * generator returns with {@code @Id} and {@link GeneratedValue @GeneratedValue} and
 * specify the generator class.
 *
 * <pre>
 * &#64;Node
 * public class MyEntity {
 *     &#64;Id &#64;GeneratedValue(UUIDStringGenerator.class) String theId;
 * }
 * </pre>
 *
 * Externally generated ids are indistinguishable to assigned ids from the database
 * perspective and thus can be arbitrarily named via {@code @Property}.
 *
 * @author Michael J. Simons
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Documented
@Inherited
@org.springframework.data.annotation.Id
@API(status = API.Status.STABLE, since = "1.0")
public @interface Id {

}
