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

package org.springframework.data.neo4j.mapping;

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;

import java.util.Collection;

/**
 * Interface for Neo4J specific {@link PersistentEntity}.
 * 
 * @author Oliver Gierke
 */
public interface Neo4jPersistentEntity<T> extends PersistentEntity<T, Neo4jPersistentProperty> {

    boolean useShortNames();

    boolean isNodeEntity();

    boolean isRelationshipEntity();

    void setPersistentState(Object entity, PropertyContainer pc);

    Object getPersistentId(Object entity);

    RelationshipProperties getRelationshipProperties();

    MappingPolicy getMappingPolicy();

    StoredEntityType getEntityType();

    Neo4jPersistentProperty getUniqueProperty();

    boolean isUnique();

    Collection<String> getAllLabels();
}
