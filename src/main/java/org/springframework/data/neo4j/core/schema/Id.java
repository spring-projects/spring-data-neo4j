/*
 * Copyright 2011-2021 the original author or authors.
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
package org.springframework.data.neo4j.core.schema;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * This annotation is included here for completeness. It marks an attribute as the primary id of a node entity. It can
 * be used as an alternative to {@link org.springframework.data.annotation.Id} and it may provide additional features in
 * the future.
 * <p>
 * To use assigned ids, annotate an arbitrary attribute of your domain class with
 * {@link org.springframework.data.annotation.Id} or this annotation:
 *
 * <pre>
 * &#64;Node
 * public class MyEntity {
 * 	&#64;Id String theId;
 * }
 * </pre>
 *
 * You can combine {@code @Id} with {@code @Property} with assigned ids to rename the node property in which the
 * assigned id is stored.
 * <p>
 * To use internally generated ids, annotate an arbitrary attribute of type {@code java.lang.long} or
 * {@code java.lang.Long} with {@code @Id} and {@link GeneratedValue @GeneratedValue}.
 *
 * <pre>
 * &#64;Node
 * public class MyEntity {
 * 	&#64;Id &#64;GeneratedValue Long id;
 * }
 * </pre>
 *
 * It does not need to be named {@code id}, but most people chose this as the attribute in the class. As the attribute
 * does not correspond to a node property, it cannot be renamed via {@code @Property}.
 * <p>
 * To use externally generated ids, annotate an arbitrary attribute with a type that your generated returns with
 * {@code @Id} and {@link GeneratedValue @GeneratedValue} and specify the generator class.
 *
 * <pre>
 * &#64;Node
 * public class MyEntity {
 * 	&#64;Id &#64;GeneratedValue(UUIDStringGenerator.class) String theId;
 * }
 * </pre>
 *
 * Externally generated ids are indistinguishable to assigned ids from the database perspective and thus can be
 * arbitrarily named via {@code @Property}.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Documented
@Inherited
@org.springframework.data.annotation.Id
@API(status = API.Status.STABLE, since = "6.0")
public @interface Id {
}
