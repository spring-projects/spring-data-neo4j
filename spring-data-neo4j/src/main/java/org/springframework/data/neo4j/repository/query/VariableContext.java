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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.util.StringUtils;

/**
 *
 * @author Oliver Gierke
 */
public class VariableContext {

    private Map<PersistentPropertyPath<Neo4jPersistentProperty>, String> variables;

    public VariableContext() {
        this.variables = new HashMap<PersistentPropertyPath<Neo4jPersistentProperty>, String>();
    }

    public String getVariableFor(PersistentPropertyPath<Neo4jPersistentProperty> path) {

        if (variables.containsKey(path)) {
            return variables.get(path);
        }

        Neo4jPersistentProperty baseProperty = path.getBaseProperty();
        List<String> parts = new ArrayList<String>();
        parts.add(getVariableFor(baseProperty.getOwner()));

        final Neo4jPersistentProperty leaf = path.getLeafProperty();
        for (Neo4jPersistentProperty property : path) {
            if (leaf.isRelationship() || !leaf.equals(property)) {
                parts.add(property.getName());
            }
        }

        String variable = StringUtils.collectionToDelimitedString(parts, "_");
        variables.put(path, variable);
        return variable;
    }

    public String getVariableFor(Neo4jPersistentEntity<?> entity) {
        return StringUtils.uncapitalize(entity.getType().getSimpleName());
    }
}
