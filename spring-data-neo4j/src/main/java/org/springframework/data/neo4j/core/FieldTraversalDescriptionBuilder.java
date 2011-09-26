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

package org.springframework.data.neo4j.core;

import org.neo4j.graphdb.traversal.TraversalDescription;
import org.springframework.data.neo4j.mapping.Neo4JPersistentProperty;

/**
 * Interface for classes that build traversal descriptions. Those classes can be referred to by
 * {@link org.springframework.data.neo4j.annotation.GraphTraversal#traversalBuilder()} to  provide fields that return
 * a dynamic traversal on access.
 *
 * @author Michael Hunger
 * @since 15.09.2010
 */
public interface FieldTraversalDescriptionBuilder {
    /**
     * Builder method for traversal description.
     *
     * @param start the Entity that contains the field with the dynamic traversal. Used for the parametrization of the traversal description.
     * @param property
     * @return the TraversalDescription to apply on fieldaccess, the start node is the current entity node
     */
    TraversalDescription build(NodeBacked start, Neo4JPersistentProperty property, String...params);
}
