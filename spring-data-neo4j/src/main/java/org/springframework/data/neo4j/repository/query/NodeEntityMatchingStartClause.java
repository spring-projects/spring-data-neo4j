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

import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.repository.query.Parameter;

import java.util.Map;

public class NodeEntityMatchingStartClause extends StartClause {
    public NodeEntityMatchingStartClause(PartInfo partInfo) {
        super(partInfo);
    }

    @Override
    public String toString() {
        return String.format(QueryTemplates.START_NODE_LOOKUP, getPartInfo().getIdentifier(), getPartInfo().getParameterIndex());
    }

    @Override
    public Map<Parameter, Object> resolveParameters(Map<Parameter, Object> parameters, Neo4jTemplate template) {
        return parameters;
    }
}
