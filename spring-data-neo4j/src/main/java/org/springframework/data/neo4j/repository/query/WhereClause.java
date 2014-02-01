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

import java.util.*;

import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.neo4j.fieldaccess.PropertyConverter;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.parser.Part;
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

    protected static final Map<Type, String> SYMBOLS;

    static {

        Map<Type, String> symbols = new HashMap<Type, String>();
        symbols.put(Type.GREATER_THAN, ">");
        symbols.put(Type.AFTER, ">");
        symbols.put(Type.GREATER_THAN_EQUAL, ">=");
        symbols.put(Type.LESS_THAN, "<");
        symbols.put(Type.BEFORE, "<");
        symbols.put(Type.LESS_THAN_EQUAL, "<=");
        symbols.put(Type.NEGATING_SIMPLE_PROPERTY, "<>");

        symbols.put(Type.SIMPLE_PROPERTY, "=");

        symbols.put(Type.REGEX, "=~");
        symbols.put(Type.STARTING_WITH, "=~");
        symbols.put(Type.CONTAINING, "=~");
        symbols.put(Type.ENDING_WITH, "=~");
        symbols.put(Type.LIKE, "=~"); // n.name =~ "Tob.*"
        symbols.put(Type.NOT_LIKE, "=~");
        symbols.put(Type.EXISTS, ""); // property exists n.name
        symbols.put(Type.IS_NULL, "is null");
        symbols.put(Type.IN, "in");
        symbols.put(Type.NOT_IN, "in");
        symbols.put(Type.TRUE, "= true");
        symbols.put(Type.FALSE, "= false");
        SYMBOLS = Collections.unmodifiableMap(symbols);
    }

    protected final PartInfo partInfo;
    protected final Type type;
    private PropertyConverter propertyConverter;

    public WhereClause(PartInfo partInfo, Neo4jTemplate template) {
        Assert.notNull(partInfo.getType());
        this.partInfo = partInfo;
        this.type = this.partInfo.getType();
        Neo4jPersistentProperty property = partInfo.getLeafProperty();
        if (!property.isNeo4jPropertyType()) {
            propertyConverter = new PropertyConverter(template.getConversionService(), property);
        }
    }

    @Override
    public String toString() {
        final String propertyName = partInfo.getNeo4jPropertyName();
        final String operator = SYMBOLS.get(type);
        final String variable = partInfo.getIdentifier();

        String result;
        if (type.getNumberOfArguments()==0) {
            result = String.format(WHERE_CLAUSE_0, variable, propertyName, operator);
        } else {
            result = String.format(WHERE_CLAUSE_1, variable, propertyName, operator, partInfo.getParameterIndex());
        }
        if (type==Type.EXISTS) {
            result = "has("+ result +")";
        }
        if (EnumSet.of(Type.NOT_IN,Type.NOT_LIKE).contains(type)) {
            result = "not( "+result+" )";
        }
        return result;
    }

    public PartInfo getPartInfo() {
        return partInfo;
    }

    public Map<Parameter, Object> resolveParameters(Map<Parameter, Object> parameters) {
        for (Map.Entry<Parameter, Object> entry : parameters.entrySet()) {
            if (partInfo.getParameterIndex() == entry.getKey().getIndex()) {
                entry.setValue(convertValue(partInfo, entry.getValue()));
            }
        }
        return parameters;
    }

    protected Object convertValue(PartInfo partInfo, Object value) {
        if (EnumSet.of(Type.CONTAINING, Type.STARTING_WITH, Type.ENDING_WITH).contains(type))
            return QueryTemplates.formatExpression(this.partInfo, value);
        else if (propertyConverter!=null) {
            return propertyConverter.serializePropertyValue(value);
        }
        return value;
    }
}
