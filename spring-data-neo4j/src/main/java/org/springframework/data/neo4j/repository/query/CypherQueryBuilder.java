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

import static org.springframework.util.StringUtils.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.util.Assert;

/**
 * Value object to create Cypher queries.
 * 
 * @author Oliver Gierke
 */
class CypherQueryBuilder implements CypherQueryDefinition {

    private final MappingContext<? extends Neo4jPersistentEntity<?>, Neo4jPersistentProperty> context;

    private final VariableContext variableContext = new VariableContext();
    private final List<MatchClause> matchClauses = new ArrayList<MatchClause>();
    private final List<StartClause> startClauses = new ArrayList<StartClause>();
    private final List<WhereClause> whereClauses = new ArrayList<WhereClause>();

    private int index = 0;
    private final Neo4jPersistentEntity<?> entity;

    /**
     * Creates a new {@link CypherQueryBuilder}.
     * 
     * @param context must not be {@literal null}.
     * @param type must not be {@literal null}.
     */
    public CypherQueryBuilder(MappingContext<? extends Neo4jPersistentEntity<?>, Neo4jPersistentProperty> context, Class<?> type) {
        Assert.notNull(context);
        Assert.notNull(type);

        this.context = context;
        this.entity = context.getPersistentEntity(type);
    }

    private String defaultStartClause() {
        return String.format(QueryTemplates.DEFAULT_START_CLAUSE, this.variableContext.getVariableFor(entity), entity
                .getType().getName());
    }

    /**
     * Adds the given {@link Part} to the restrictions for the query.
     * 
     * @param part
     * @return
     */
    public CypherQueryBuilder addRestriction(Part part) {

        PersistentPropertyPath<Neo4jPersistentProperty> path = context.getPersistentPropertyPath(part.getProperty());
        String variable = variableContext.getVariableFor(path);

        final PartInfo partInfo = new PartInfo(path, variable, part, index);
        if (partInfo.isPrimitiveProperty()) {
            if (partInfo.isIndexed()) {
                startClauses.add(new StartClause(partInfo));
            } else {
                whereClauses.add(new WhereClause(path, variable, part.getType(), index, partInfo));
            }
        }
        index += 1;

        MatchClause matchClause = new MatchClause(path);

        if (matchClause.hasRelationship()) {
            matchClauses.add(matchClause);
        }

        return this;
    }

    public PartInfo getPartInfo(int parameterIndex) {
        for (StartClause startClause : startClauses) {
            if (startClause.getPartInfo().getParameterIndex()==parameterIndex) return startClause.getPartInfo();
        }
        for (WhereClause whereClause : whereClauses) {
            if (whereClause.getPartInfo().getParameterIndex()==parameterIndex) return whereClause.getPartInfo();
        }
        throw new IllegalArgumentException("Index "+parameterIndex+" not valid");
    }


    /*
    * (non-Javadoc)
    * @see java.lang.Object#toString()
    */
    @Override
    public String toString() {

        String startClauses = collectionToDelimitedString(this.startClauses, ", ");
        String matchClauses = toString(this.matchClauses);
        String whereClauses = collectionToDelimitedString(this.whereClauses, " and ");

        StringBuilder builder = new StringBuilder("start ");

        if (hasText(startClauses)) {
            builder.append(startClauses);
        } else {
            builder.append(defaultStartClause());
        }

        if (hasText(matchClauses)) {
            builder.append(" match ").append(matchClauses);
        }

        if (hasText(whereClauses)) {
            builder.append(" where ").append(whereClauses);
        }

        builder.append(" return ").append(variableContext.getVariableFor(entity));
        return builder.toString();
    }

    /* (non-Javadoc)
     * @see org.springframework.data.neo4j.repository.query.CypherQueryDefinition#toString(org.springframework.data.domain.Pageable)
     */
    @Override
    public String toString(Pageable pageable) {

        StringBuilder builder = new StringBuilder(toString(pageable.getSort()));

        if (pageable != null) {
            builder.append(String.format(QueryTemplates.SKIP_LIMIT, pageable.getOffset(), pageable.getPageSize()));
        }

        return builder.toString();
    }

    /* (non-Javadoc)
     * @see org.springframework.data.neo4j.repository.query.CypherQueryDefinition#toString(org.springframework.data.domain.Sort)
     */
    @Override
    public String toString(Sort sort) {
        StringBuilder builder = new StringBuilder(toString());
        builder.append(addSorts(sort));
        return builder.toString();
    }

    private String addSorts(Sort sort) {
        final List<String> sorts = formatSorts(sort);
        return !sorts.isEmpty() ? String
                .format(QueryTemplates.ORDER_BY_CLAUSE, collectionToCommaDelimitedString(sorts)) : "";
    }

    private List<String> formatSorts(Sort sort) {
        List<String> result = new ArrayList<String>();
        if (sort == null) {
            return result;
        }

        for (Sort.Order order : sort) {
            result.add(String.format(QueryTemplates.SORT_CLAUSE, order.getProperty(), order.getDirection()));
        }
        return result;
    }

    private String toString(List<MatchClause> matchClauses) {
        List<String> result = new ArrayList<String>(matchClauses.size());
        for (MatchClause matchClause : matchClauses) {
            result.add(matchClause.toString(variableContext));
        }
        return collectionToDelimitedString(result, ", ");
    }
}
