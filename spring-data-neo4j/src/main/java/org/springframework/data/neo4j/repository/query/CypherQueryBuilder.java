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

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.util.StringUtils.*;

/**
 * Value object to create Cypher queries.
 *
 * @author Oliver Gierke
 */
class CypherQueryBuilder {

    public static class CypherQuery implements CypherQueryDefinition {
        private final VariableContext variableContext = new VariableContext();
        private final List<MatchClause> matchClauses = new ArrayList<MatchClause>();
        private final List<StartClause> startClauses = new ArrayList<StartClause>();
        private final List<WhereClause> whereClauses = new ArrayList<WhereClause>();
        private int index = 0;
        private final Neo4jPersistentEntity<?> entity;

        public CypherQuery(final Neo4jPersistentEntity<?> entity) {
            this.entity = entity;
        }

        private String getEntityName(Neo4jPersistentEntity<?> entity) {
            return variableContext.getVariableFor(entity);
        }

        private String defaultStartClause(Neo4jPersistentEntity<?> entity) {
            return String.format(QueryTemplates.DEFAULT_START_CLAUSE, getEntityName(entity), entity
                    .getEntityType().getAlias());
        }

        public void addPart(Part part, PersistentPropertyPath<Neo4jPersistentProperty> path) {
            String variable = variableContext.getVariableFor(path);

            final PartInfo partInfo = new PartInfo(path, variable, part, index);
            // index("a:foo AND b:bar")
            // a=index1(a="foo"), b=index2(b="bar") where a=b - not good b/c of cross product
            // index1(a=foo) where a.foo=bar
            if (partInfo.isPrimitiveProperty()) {
                if (!addedStartClause(partInfo)) {
                    whereClauses.add(new WhereClause(path, variable, part.getType(), index, partInfo));
                }
            }
            index += 1;

            MatchClause matchClause = new MatchClause(path);

            if (matchClause.hasRelationship()) {
                matchClauses.add(matchClause);
            }
        }

        private boolean addedStartClause(PartInfo partInfo) {
            if (!partInfo.isIndexed()) return false;
            for (StartClause startClause : startClauses) {
                // only merge when same index and same variable
                if (startClause.sameIdentifier(partInfo)) {
                    if (startClause.sameIndex(partInfo)) {
                        startClause.merge(partInfo);
                        return true;
                    } else {
                        // must stop b/c of invalid combination (same identifier but different index)
                        return false;
                    }
                }
            }
            startClauses.add(new StartClause(partInfo));
            return true;
        }

        public PartInfo getPartInfo(int parameterIndex) {
            for (StartClause startClause : startClauses) {
                if (startClause.getPartInfo().getParameterIndex() == parameterIndex) return startClause.getPartInfo();
            }
            for (WhereClause whereClause : whereClauses) {
                if (whereClause.getPartInfo().getParameterIndex() == parameterIndex) return whereClause.getPartInfo();
            }
            throw new IllegalArgumentException("Index " + parameterIndex + " not valid");
        }

        @Override
        public Map<Parameter, Object> resolveParameters(Map<Parameter, Object> parameters) {
            for (StartClause startClause : startClauses) {
                startClause.resolveParameters(parameters);
            }
            for (MatchClause matchClause : matchClauses) {
                matchClause.resolveParameters(parameters);
            }
            for (WhereClause whereClause : whereClauses) {
                whereClause.resolveParameters(parameters);
            }

            for (Map.Entry<Parameter, Object> entry : parameters.entrySet()) {
                final Parameter parameter = entry.getKey();
                PartInfo info = getPartInfo(parameter.getIndex());
                if (info.isFullText()) {
                    Object newValue = String.format(QueryTemplates.PARAMETER_INDEX_QUERY, info.getIndexKey(), entry.getValue());
                    entry.setValue(newValue);
                }
            }
        }

        @Override
        public String toString() {
            String startClauses = collectionToDelimitedString(this.startClauses, ", ");
            String matchClauses = toString(this.matchClauses);
            String whereClauses = collectionToDelimitedString(this.whereClauses, " and ");

            StringBuilder builder = new StringBuilder("start ");

            if (hasText(startClauses)) {
                builder.append(startClauses);
            } else {
                builder.append(defaultStartClause(entity));
            }

            if (hasText(matchClauses)) {
                builder.append(" match ").append(matchClauses);
            }

            if (hasText(whereClauses)) {
                builder.append(" where ").append(whereClauses);
            }

            builder.append(" return ").append(String.format(QueryTemplates.VARIABLE, getEntityName(entity)));
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

        @Override
        public String toString(Sort sort) {
            StringBuilder builder = new StringBuilder(toString());
            if (sort != null) builder.append(addSorts(sort));
            return builder.toString();
        }

        @Override
        public String toString(Pageable pageable) {
            if (pageable == null) {
                return toString();
            }
            StringBuilder builder = new StringBuilder(toString(pageable.getSort()));
            builder.append(String.format(QueryTemplates.SKIP_LIMIT, pageable.getOffset(), pageable.getPageSize()));
            return builder.toString();
        }
    }

    private final MappingContext<? extends Neo4jPersistentEntity<?>, Neo4jPersistentProperty> context;
    private final CypherQuery query;

    /**
     * Creates a new {@link CypherQueryBuilder}.
     *
     * @param context must not be {@literal null}.
     * @param type    must not be {@literal null}.
     */
    public CypherQueryBuilder(MappingContext<? extends Neo4jPersistentEntity<?>, Neo4jPersistentProperty> context, Class<?> type) {
        Assert.notNull(context);
        Assert.notNull(type);

        this.context = context;
        Neo4jPersistentEntity<?> entity = context.getPersistentEntity(type);
        this.query = new CypherQuery(entity);
    }

    public CypherQueryBuilder addRestriction(Part part) {
        PersistentPropertyPath<Neo4jPersistentProperty> path = context.getPersistentPropertyPath(part.getProperty());
        query.addPart(part, path);
        return this;
    }


    public CypherQueryDefinition buildQuery(Sort sort) {
        return query;
    }
}
