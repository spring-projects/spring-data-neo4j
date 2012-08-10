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

import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;

/**
 * Representation of a Cypher {@literal start} clause.
 * 
 * @author Oliver Gierke
 */
class StartClause {

    private final PartInfo partInfo;

    /**
     * Creates a new {@link StartClause} from the given {@link Neo4jPersistentProperty}, variable and the given
     * parameter index.
     *
     * @param partInfo
     */
    public StartClause(PartInfo partInfo) {
        this.partInfo = partInfo;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final String variable = partInfo.getVariable();
        final String indexName = partInfo.getIndexName();
        final int parameterIndex = partInfo.getParameterIndex();
        if (partInfo.isFullText()) {
            return String.format(QueryTemplates.START_CLAUSE_FULLTEXT, variable, indexName, parameterIndex);
        }
        return String.format(QueryTemplates.START_CLAUSE, variable, indexName, partInfo.getNeo4jPropertyName(), parameterIndex);
    }

    public PartInfo getPartInfo() {
        return partInfo;
    }

    public void merge(PartInfo partInfo) {

    }
}