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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.util.Assert;

import static org.springframework.data.neo4j.repository.query.QueryTemplates.WHERE_CLAUSE_0;
import static org.springframework.data.neo4j.repository.query.QueryTemplates.WHERE_CLAUSE_1;

/**
 * Representation of a Cypher {@literal where} clause.
 * 
 * @author Oliver Gierke
 */
class WhereClause {

    private static final Map<Type, String> SYMBOLS;

    static {

        Map<Type, String> symbols = new HashMap<Type, String>();
        symbols.put(Type.GREATER_THAN, ">");
        symbols.put(Type.GREATER_THAN_EQUAL, ">=");
        symbols.put(Type.LESS_THAN, "<");
        symbols.put(Type.LESS_THAN_EQUAL, "<=");
        symbols.put(Type.NEGATING_SIMPLE_PROPERTY, "!=");
        symbols.put(Type.SIMPLE_PROPERTY, "=");
        symbols.put(Type.LIKE, "=~"); // n.name =~ /Tob.*/
        //symbols.put(Type.EXISTS, ""); // property exists n.name
                                      // compare when exists WHERE n.belt? = 'white'
        symbols.put(Type.IS_NULL, "is null"); // WHERE r is null
        SYMBOLS = Collections.unmodifiableMap(symbols);
    }

    private final PartInfo partInfo;

    /**
     * Creates a new {@link WhereClause} for the given {@link Neo4jPersistentProperty}, variable, type and parameter
     * index.
     *
     * @param partInfo
     */
    public WhereClause(PersistentPropertyPath<Neo4jPersistentProperty> path, String variable, Type type, int index, PartInfo partInfo) {
        Assert.notNull(partInfo.getType());
        this.partInfo = partInfo;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final String propertyName = partInfo.getNeo4jPropertyName();
        final String operator = SYMBOLS.get(partInfo.getType());
        final String variable = partInfo.getIdentifier();

        if (partInfo.getType().getNumberOfArguments()==0) {
            return String.format(WHERE_CLAUSE_0, variable, propertyName, operator);
        }
        return String.format(WHERE_CLAUSE_1, variable, propertyName, operator, partInfo.getParameterIndex());
    }

    public PartInfo getPartInfo() {
        return partInfo;
    }
}
