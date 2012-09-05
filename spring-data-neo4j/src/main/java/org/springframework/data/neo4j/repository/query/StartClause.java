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

import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.neo4j.fieldaccess.PropertyConverter;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.parser.Part;

import java.util.*;

/**
 * Representation of a Cypher {@literal start} clause.
 * 
 * @author Oliver Gierke
 */
// TODO id-startclause, index-startclause (exact,point,fulltext)
class StartClause {

    private final SortedMap<Integer,PartInfo> partInfos=new TreeMap<Integer, PartInfo> ();

    /**
     * Creates a new {@link StartClause} from the given {@link Neo4jPersistentProperty}, variable and the given
     * parameter index.
     */
    public StartClause(PartInfo partInfo) {
        this.partInfos.put(partInfo.getParameterIndex(), partInfo);
    }

    @Override
    public String toString() {
        final PartInfo partInfo = getPartInfo();
        final String identifier = partInfo.getIdentifier();
        final String indexName = partInfo.getIndexName();
        final int parameterIndex = partInfo.getParameterIndex();
        // fulltext or multiple
        if (shouldRenderQuery()) {
            return String.format(QueryTemplates.START_CLAUSE_INDEX_QUERY, identifier, indexName, parameterIndex);
        }
        // exact and single
        return String.format(QueryTemplates.START_CLAUSE_INDEX_LOOKUP, identifier, indexName, partInfo.getNeo4jPropertyName(), parameterIndex);
    }

    private boolean shouldRenderQuery() {
        PartInfo partInfo = getPartInfo();
        return partInfo.isFullText() || EnumSet.of(Part.Type.LIKE,Part.Type.STARTING_WITH,Part.Type.CONTAINING,Part.Type.ENDING_WITH).contains(partInfo.getType())|| partInfos.size() > 1;
    }

    public Map<Parameter, Object> resolveParameters(Map<Parameter, Object> parameters, Neo4jTemplate template) {
        Map<Parameter, PartInfo> myParameters = findMyParameters(parameters.keySet());

        Map<Parameter, Object> result = new LinkedHashMap<Parameter, Object>(parameters);
        result.keySet().removeAll(myParameters.keySet());

        final Map<PartInfo, Object> values = matchToPartsAndConvert(myParameters, parameters,template);

        Parameter firstParam = IteratorUtil.first(myParameters.keySet());
        if (shouldRenderQuery()) {
            result.put(firstParam, renderQuery(values));
        } else {
            result.put(firstParam, IteratorUtil.first(values.values()));
        }
        return result;
    }

    private String renderQuery(Map<PartInfo, Object> values) {
        StringBuilder sb=new StringBuilder();
        for (Map.Entry<PartInfo, Object> entry : values.entrySet()) {
            if (sb.length()>0) sb.append(" AND ");
            final PartInfo partInfo = entry.getKey();
            Object value = entry.getValue();
            sb.append(QueryTemplates.formatIndexQuery(partInfo,value));
        }
        return sb.toString();
    }

    private Map<PartInfo, Object> matchToPartsAndConvert(Map<Parameter, PartInfo> myParameters, Map<Parameter, Object> parameters, Neo4jTemplate template) {
        Map<PartInfo, Object> result = new LinkedHashMap<PartInfo, Object>();
        for (Map.Entry<Parameter, PartInfo> entry : myParameters.entrySet()) {
            Object value = parameters.get(entry.getKey());
            PartInfo partInfo = entry.getValue();

            Neo4jPersistentProperty property = partInfo.getLeafProperty();
            result.put(partInfo,convertIfNecessary(template, value, property));
        }
        return result;
    }

    private Object convertIfNecessary(Neo4jTemplate template, Object value, Neo4jPersistentProperty property) {
        if (property.isNeo4jPropertyType() && property.isNeo4jPropertyValue(value)) return value;

        PropertyConverter converter = new PropertyConverter(template.getConversionService(), property);
        return converter.serializePropertyValue(value);
    }

    private Map<Parameter,PartInfo> findMyParameters(Set<Parameter> parameters) {
        Map<Parameter,PartInfo> result=new LinkedHashMap<Parameter, PartInfo>();
        for (Parameter parameter : parameters) {
            PartInfo partInfo = partInfos.get(parameter.getIndex());
            if (partInfo!=null) {
                result.put(parameter, partInfo);
            }
        }
        return result;
    }

    public PartInfo getPartInfo() {
        return IteratorUtil.first(partInfos.values());
    }

    public boolean merge(PartInfo partInfo) {
        for (PartInfo info : partInfos.values()) {
            if (info.sameIdentifier(partInfo) && info.sameIndex(partInfo)) {
                continue;
            }
            return false;
        }
        this.partInfos.put(partInfo.getParameterIndex(), partInfo);
        return true;
    }

    public boolean sameIdentifier(PartInfo info) {
        for (PartInfo partInfo : partInfos.values()) {
            if (!partInfo.sameIdentifier(info)) return false;
        }
        return true;
    }
    public boolean sameIndex(PartInfo info) {
        for (PartInfo partInfo : partInfos.values()) {
            if (!partInfo.sameIndex(info)) return false;
        }
        return true;
    }
}