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

import org.neo4j.graphdb.Relationship;

/**
 * concrete interface introduced onto Relationship entities by the {@link org.springframework.data.neo4j.support.relationship.Neo4jRelationshipBacking}
 * aspect, encapsulates a neo4j relationship as backing state
 */
public interface RelationshipBacked extends GraphBacked<Relationship,RelationshipBacked>{

    /**
     * @return the id of the underlying relationship or null if there is none
     */
	Long getRelationshipId();


    /**
     * Project this relationship entity as another relationship backed type. The same underlying relationship will be
     * used for the new entity.
     *
     * @param targetType type to project to
     * @return new instance of specified type, sharing the same underlying relationship with this entity
     */
    <R extends RelationshipBacked> R projectTo(Class<R> targetType);


}
