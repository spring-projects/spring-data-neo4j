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
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.repository.query.Parameter;
import scala.annotation.target.param;

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
     *
     * @param partInfo
     */
    public StartClause(PartInfo partInfo) {
        this.partInfos.put(partInfo.getParameterIndex(), partInfo);
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
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
        return getPartInfo().isFullText() || partInfos.size() > 1;
    }

    public Map<Parameter, Object> resolveParameters(Map<Parameter, Object> parameters) {
        // collect parameters grouped by key (param0)
        // convert
        Collection<Parameter> parameter = findParameter(parameters.keySet());
        Map<Parameter, Object> result = new HashMap<Parameter, Object>(parameters);
        result.keySet().removeAll(parameter);
        final Map<PartInfo, Object> values = filterForPart(parameters);
        // create query & return

        Map<Parameter, Object> result = new LinkedHashMap<Parameter, Object>();
        
        Map.Entry<Parameter, Object> firstEntry = null;
        if (shouldRenderQuery()) {
            result.put(parameter, renderQuery(values));
        } else {
            result.put(parameter, IteratorUtil.first(values.values()));
        }
        
        final int index = getPartInfo().getParameterIndex();
        if (!shouldRenderQuery()) return parameters;
        Map<Parameter, Object> result = new LinkedHashMap<Parameter, Object>();
        Map.Entry<Parameter, Object> firstEntry = null;
        for (Map.Entry<Parameter, Object> entry : parameters.entrySet()) {
            final PartInfo partInfo = partInfos.get(entry.getKey().getIndex());
            if (partInfo==null) {
                result.put(entry.getKey(), entry.getValue());
            } else {
                firstEntry = entry;
                if (sb.length()>0) sb.append(" AND ");
                sb.append(String.format(QueryTemplates.PARAMETER_INDEX_QUERY, partInfo.getNeo4jPropertyName(), entry.getValue())); // todo conversion ?
            }
        }
        if (firstEntry==null) throw new IllegalStateException("No Parameters for index start clause supplied "+toString());
        else {
            result.put(firstEntry.getKey(), firstEntry.getValue());
        }
        return result;
    }

    private void renderQuery(Map<PartInfo, Object> values) {
        StringBuilder sb=new StringBuilder();
        for (Map.Entry<PartInfo, Object> entry : values.entrySet()) {
            if (sb.length()>0) sb.append(" AND ");
            final PartInfo partInfo = entry.getKey();
            sb.append(String.format(QueryTemplates.PARAMETER_INDEX_QUERY, partInfo.getIndexKey(), entry.getValue())); // todo conversion ?
        }
    }

    private Map<PartInfo, Object> filterForPart(Map<Parameter, Object> parameters) {
        Map<PartInfo, Object> result = new LinkedHashMap<PartInfo, Object>();
        for (Map.Entry<Parameter, Object> entry : parameters.entrySet()) {
            final PartInfo partInfo = partInfos.get(entry.getKey().getIndex());
            if (partInfo!=null) {
                result.put(partInfo,converter.convert(partInfo.getType(), entry.getValue()));
            }
        }
        return result;
    }
    private Collection<Parameter> findParameter(Set<Parameter> parameters) {
        Collection<Parameter> result=new ArrayList<Parameter>();
        for (Parameter parameter : parameters) {
            if (partInfos.containsKey(parameter.getIndex())) {
                result.add(parameter);
            }
        }
        return result;
    }

    public PartInfo getPartInfo() {
        return partInfos.get(0);
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