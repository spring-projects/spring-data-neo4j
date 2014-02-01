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

import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.repository.query.parser.Part;

import java.util.EnumSet;

import static org.springframework.data.neo4j.repository.query.QueryTemplates.*;

/**
 * Representation of a Cypher {@literal where} clause specifically for
 * use to narrow the results based on particular entity types, where
 * those entities can be identified via specific Labels (as per the
 * Label Based Type Representation Strategy)
 *
 * @author Nicki Watt
 */
public class IdPropertyWhereClause extends WhereClause {



    public IdPropertyWhereClause(PartInfo partInfo, Neo4jTemplate template) {
        super(partInfo, template);
    }


    @Override
    public String toString() {
        final String operator = SYMBOLS.get(type);
        String variable = partInfo.getIdentifier();
        String result = String.format(WHERE_CLAUSE_ID, variable, operator, partInfo.getParameterIndex());
        if (EnumSet.of(Part.Type.NOT_IN).contains(type)) {
            result = "not( "+result+" )";
        }
        return result;
    }

    @Override
    protected Object convertValue(PartInfo partInfo, Object value) {
        return value;
    }
}
