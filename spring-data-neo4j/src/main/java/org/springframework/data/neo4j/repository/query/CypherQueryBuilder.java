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
package org.springframework.data.neo4j.repository.query;

import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.repository.query.parser.Part;

/**
 * Value object to create Cypher queries.
 *
 * @author Oliver Gierke
 */
class CypherQueryBuilder {

    private final MappingContext<? extends Neo4jPersistentEntity<?>, Neo4jPersistentProperty> context;
    private final CypherQuery query;

    public CypherQueryBuilder(MappingContext<? extends Neo4jPersistentEntity<?>, Neo4jPersistentProperty> context, Class<?> type, Neo4jTemplate template) {
        this.context = context;
        Neo4jPersistentEntity<?> entity = context.getPersistentEntity(type);
        this.query = new CypherQuery(entity, template, template.isLabelBased());
    }

    public CypherQueryBuilder asCountQuery() {
        query.setIsCountQuery(true);
        return this;
    }

    public CypherQueryBuilder addRestriction(Part part) {
        query.addPart(part, context.getPersistentPropertyPath(part.getProperty()));
        return this;
    }

    public CypherQueryDefinition buildQuery(Sort sort) {
        return query.withSort(sort);
    }

    public CypherQueryDefinition buildQuery() {
        return query;
    }

    @Override
    public String toString() {
        return query.toQueryString();
    }
}
