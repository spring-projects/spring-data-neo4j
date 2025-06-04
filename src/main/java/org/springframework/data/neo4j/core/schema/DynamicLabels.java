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
package org.springframework.data.neo4j.core.schema;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * This annotation can be used on a field of type {@link java.util.Collection
 * Collection&lt;String&gt;}. The content of this field will be treated as dynamic or
 * runtime managed labels. This means: All labels that are not statically defined via the
 * class hierarchy and the corresponding {@link Node @Node} annotation are added to this
 * list while loading the entity and all values contained in the collection will be added
 * to the nodes labels.
 * <p>
 * Labels not defined through the class hierarchy or the list of dynamic labels will be
 * removed from the database when {@link DynamicLabels @DynamicLabels} is used.
 *
 * @author Michael J. Simons
 * @since 6.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
@API(status = API.Status.STABLE, since = "6.0")
public @interface DynamicLabels {

}
