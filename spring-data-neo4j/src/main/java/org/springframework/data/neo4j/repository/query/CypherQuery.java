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
import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.parser.Part;

import java.util.*;

import static org.springframework.util.StringUtils.*;
import static org.springframework.util.StringUtils.hasText;

public class CypherQuery implements CypherQueryDefinition {
    private final VariableContext variableContext = new VariableContext();
    private final List<MatchClause> matchClauses = new ArrayList<MatchClause>();
    private final List<StartClause> startClauses = new ArrayList<StartClause>();
    private final List<WhereClause> whereClauses = new ArrayList<WhereClause>();
    private Sort defaultSorts;
    private int index = 0;
    private final Neo4jPersistentEntity<?> entity;
    private final Neo4jTemplate template;
    private boolean isCountQuery = false;
    private boolean useLabels = false;

    public CypherQuery(final Neo4jPersistentEntity<?> entity, Neo4jTemplate template, boolean useLabels) {
        this.entity = entity;
        this.template = template;
        this.useLabels = useLabels;
    }

    private String getEntityName(Neo4jPersistentEntity<?> entity) {
        return variableContext.getVariableFor(entity);
    }

    private String defaultIndexBasedStartClause(Neo4jPersistentEntity<?> entity) {
        return  String.format(QueryTemplates.DEFAULT_INDEXBASED_START_CLAUSE,
                getEntityName(entity),
                entity.getEntityType().getAlias());
    }

    private String defaultMatchBasedStartClause(Neo4jPersistentEntity<?> entity) {
        return  String.format(QueryTemplates.DEFAULT_LABELBASED_MATCH_START_CLAUSE,
                getEntityName(entity),
                toLabelString(entity.getAllLabels()));
    }

    private String toLabelString(Collection<String> labels) {
        if (labels==null || labels.isEmpty()) return "";
        StringBuilder sb=new StringBuilder();
        for (String label : labels) {
            sb.append(":`").append(label).append("`");
        }
        return sb.toString();
    }

    public void addPart(Part part, PersistentPropertyPath<Neo4jPersistentProperty> path) {
        String variable = variableContext.getVariableFor(path);

        final PartInfo partInfo = new PartInfo(path, variable, part, index);
        MatchClause matchClause = new MatchClause(path);
        // index("a:foo AND b:bar")
        // a=index1(a="foo"), b=index2(b="bar") where a=b - not good b/c of cross product
        // index1(a=foo) where a.foo=bar
        Neo4jPersistentProperty leafProperty = partInfo.getLeafProperty();
        boolean isIdProperty = leafProperty.isIdProperty();
        boolean addedMatchClause = false;
        if (partInfo.isPrimitiveProperty() && !isIdProperty) {
            if (!addedStartClause(partInfo)) {
                whereClauses.add(new WhereClause(partInfo,template));
            }
        } else if (leafProperty.isRelationship() || isIdProperty) {
            if (useLabels) {
                whereClauses.add(new IdPropertyWhereClause(new PartInfo(path, variable, part, index), template));
                whereClauses.add(new LabelBasedTypeRestrictingWhereClause(new PartInfo(path, variableContext.getVariableFor(entity), part, -1), entity, template));
                matchClauses.add(matchClause);
                addedMatchClause = true;
            } else {
                startClauses.add(new GraphIdStartClause(partInfo));
                whereClauses.add(new IndexBasedTypeRestrictingWhereClause(new PartInfo(path, variableContext.getVariableFor(entity), part, -1), entity, template));
            }
        } else {
            throw new IllegalStateException("Error "+part+" points neither to a primitive nor a entity property of "+entity);
        }
        index += 1;

        if (!addedMatchClause && matchClause.hasRelationship()) {
            matchClauses.add(matchClause);
        }
    }

    public CypherQueryDefinition withSort(Sort sorts) {
        this.defaultSorts=sorts;
        return this;
    }

    private Sort getCypherEntityRefAwareSort(Sort sorts) {
        List<Sort.Order> entityAwareOrders = new ArrayList<Sort.Order>();
        for (Sort.Order o : sorts) {
            entityAwareOrders.add( getEntityAwareOrderRef(o) );
        }
        return new Sort(entityAwareOrders);
    }

    private Sort.Order getEntityAwareOrderRef(Sort.Order o) {
        // Assumes a string with no period refers to a property on entity
        return (o.getProperty().contains("."))
            ? o
            : new Sort.Order(o.getDirection(),getEntityName(entity)+"."+o.getProperty());
    }

    private boolean addedStartClause(PartInfo partInfo) {

        if (!partInfo.isIndexed() || partInfo.isLabelIndexed()) return false;

        ListIterator<StartClause> it = startClauses.listIterator();
        while (it.hasNext()) {
            StartClause startClause = it.next();
            if (startClause.sameIdentifier(partInfo)) {
                if (startClause.sameIndex(partInfo)) {
                    startClause.merge(partInfo);
                    if (startClause.hasMultipleParts()) {
                        // Replace it as we could not detect this initially ...
                        List<PartInfo> newList = new ArrayList<PartInfo>();
                        newList.addAll(startClause.getPartInfos());
                        it.set(StartClauseFactory.create(newList));
                    }
                    return true;
                } else {
                    // must stop b/c of invalid combination (same identifier but different index)
                    return false;
                }
            }
        }
        startClauses.add(StartClauseFactory.create(partInfo));
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
            parameters = startClause.resolveParameters(parameters,template);
        }
        for (MatchClause matchClause : matchClauses) {
            parameters = matchClause.resolveParameters(parameters);
        }
        for (WhereClause whereClause : whereClauses) {
            parameters = whereClause.resolveParameters(parameters);
        }
        return parameters;
    }

    @Override
    public String toQueryString() {
        return toQueryString(defaultSorts);
    }

    private String render() {

        String startClauses = collectionToDelimitedString(this.startClauses, ", ");
        String matchClauses = toQueryString(this.matchClauses);
        String whereClauses = collectionToDelimitedString(this.whereClauses, " AND ");

        StringBuilder builder = new StringBuilder("");
        boolean startClauseInUse = renderStartClauses(builder, startClauses);
        renderMatchClauses(builder, matchClauses, startClauseInUse);
        renderWhereClauses(builder, whereClauses);
        renderReturnClauses(builder);

        return builder.toString();
    }

    private void renderReturnClauses(StringBuilder builder) {
        String returnEntity = String.format(QueryTemplates.VARIABLE,getEntityName(entity));
        if (isCountQuery) {
            builder.append(" RETURN ").append("count(").append(returnEntity).append(")");
        } else {
            builder.append(" RETURN ").append(returnEntity);
        }
    }

    private void renderWhereClauses(StringBuilder builder, String whereClauses) {
        if (hasText(whereClauses)) {
            builder.append(" WHERE ").append(whereClauses);
        }
    }

    private void renderMatchClauses(StringBuilder builder, String matchClauses, boolean startClauseInUse) {
        if (hasText(matchClauses)) {
            builder.append(" MATCH ").append(matchClauses);
            return;
        }
        if (useLabelBasedTRS() && !startClauseInUse) {
            builder.append(" MATCH ").append(defaultMatchBasedStartClause(entity));
            return;
        }
    }

    /**
     * From Neo4j 2.0 onwards the start clause is optional (although still needs to be
     * used for certain cases where index lookups are required etc. This method takes
     * the currently built up query in builder (which should be empty at this point)
     * and begins to add in any currently defined start clauses. In the absence of
     * any start clauses, it will add in the default start clause if using an indexing
     * strtegy.
     *
     * @param builder current query which has been built up
     * @param startClauses List of current start clauses
     * @return true if this method resulted in the query having a START clause
     *         applied to it otherwise false
     */
    private boolean renderStartClauses(StringBuilder builder, String startClauses) {
        if (hasText(startClauses)) {
            builder.append("START ").append(startClauses);
            return true;
        }
        if (useIndexBasedTRS()) {
            builder.append("START ").append(defaultIndexBasedStartClause(entity));
            return true;
        }

        // For a label based strategy, we do not use a start clause
        // but rather begin with a MATCH clause
        return false;
    }

    private boolean useIndexBasedTRS() {
        return !useLabels;
    }

    private boolean useLabelBasedTRS() {
        return useLabels;
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

    private String toQueryString(List<MatchClause> matchClauses) {
        List<String> result = new ArrayList<String>(matchClauses.size());
        for (MatchClause matchClause : matchClauses) {
            result.add(matchClause.toString(variableContext));
        }
        return collectionToDelimitedString(result, ", ");
    }

    @Override
    public String toQueryString(Sort sort) {
        return toQueryString(sort,true);
    }

    private String toQueryString(Sort sort,boolean applyMissingRefs) {
        StringBuilder builder = new StringBuilder(render());
        if (sort != null) {
            builder.append(addSorts(
                    applyMissingRefs ? getCypherEntityRefAwareSort(sort) : sort));
        }
        return builder.toString().trim();
    }

    @Override
    public String toQueryString(Pageable pageable) {
        if (pageable == null) {
            return render();
        }
        StringBuilder builder = new StringBuilder(toQueryString(pageable.getSort()));
        builder.append(String.format(QueryTemplates.SKIP_LIMIT, pageable.getOffset(), pageable.getPageSize()));
        return builder.toString().trim();
    }

    @Override
    public String toString() {
        return toQueryString();
    }

    public void setIsCountQuery(boolean isCountQuery) {
        this.isCountQuery = isCountQuery;
    }
}
