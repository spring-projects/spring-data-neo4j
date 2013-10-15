/**
 * Copyright 2013 the original author or authors.
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

import org.neo4j.index.lucene.ValueContext;
import org.springframework.data.neo4j.fieldaccess.PropertyConverter;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.repository.query.Parameter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Abstract class which represents a start clause which makes
 * use of an index of some sort.
 *
 * @author Nicki Watt
 */
abstract class IndexBasedStartClause extends StartClause {

    public IndexBasedStartClause(PartInfo partInfo) {
        super(partInfo);
    }

    protected Map<PartInfo, Object> matchToPartsAndConvert(Map<Parameter, PartInfo> myParameters, Map<Parameter, Object> parameters, Neo4jTemplate template) {
        Map<PartInfo, Object> result = new LinkedHashMap<PartInfo, Object>();
        for (Map.Entry<Parameter, PartInfo> entry : myParameters.entrySet()) {
            Object value = parameters.get(entry.getKey());
            PartInfo partInfo = entry.getValue();

            Neo4jPersistentProperty property = partInfo.getLeafProperty();
            result.put(partInfo,convertIfNecessary(template, value, property));
        }
        return result;
    }

    protected Object convertIfNecessary(Neo4jTemplate template, Object value, Neo4jPersistentProperty property) {
        if (property.isIndexedNumerically()) return new ValueContext(value).indexNumeric();
        if (property.isNeo4jPropertyType() && property.isNeo4jPropertyValue(value)) return value;

        PropertyConverter converter = new PropertyConverter(template.getConversionService(), property);
        return converter.serializePropertyValue(value);
    }

    protected Map<Parameter,PartInfo> findMyParameters(Set<Parameter> parameters) {
        Map<Parameter,PartInfo> result=new LinkedHashMap<Parameter, PartInfo>();
        for (Parameter parameter : parameters) {
            PartInfo partInfo = partInfos.get(parameter.getIndex());
            if (partInfo!=null) {
                result.put(parameter, partInfo);
            }
        }
        return result;
    }


}
