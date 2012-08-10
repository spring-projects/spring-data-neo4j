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

import org.springframework.data.mapping.context.PersistentPropertyPath;
import org.springframework.data.neo4j.mapping.IndexInfo;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.util.Assert;

/**
 * @author mh
 * @since 31.10.11
 */
public class PartInfo {
    private final PersistentPropertyPath<Neo4jPersistentProperty> path;
    private final String variable;
    private final Part part;
    private final int index;

    public PartInfo(PersistentPropertyPath<Neo4jPersistentProperty> path, String variable, Part part, int index) {
        Assert.notNull(path);
        Assert.hasText(variable);

        this.path = path;
        this.variable = variable;
        this.part = part;
        this.index = index;
    }

    protected Part.Type getType() {
        return this.part.getType();
    }

    Neo4jPersistentProperty getLeafProperty() {
        return path.getLeafProperty();
    }

    public boolean isPrimitiveProperty() {
        return !isRelationship();
    }

    private boolean isRelationship() {
        return getLeafProperty().isRelationship();
    }


    public boolean isIndexed() {
        return getLeafProperty().isIndexed();
    }

    public String getVariable() {
        return variable;
    }

    public int getParameterIndex() {
        return index;
    }

    public String getIndexName() {
        return getIndexInfo().getIndexName();
    }

    public boolean isFullText() {
        return isIndexed() && getIndexInfo().isFullText();
    }

    private IndexInfo getIndexInfo() {
        return getLeafProperty().getIndexInfo();
    }

    String getNeo4jPropertyName() {
        Neo4jPersistentProperty leafProperty = getLeafProperty();
        return leafProperty.getNeo4jPropertyName();
    }

    public String getIndexKey() {
        return getIndexInfo().getIndexKey();
    }

    boolean sameIndex(PartInfo startPartInfo) {
        return startPartInfo.getIndexName().equals(getIndexName());
    }

    boolean sameVariable(PartInfo startPartInfo) {
        return startPartInfo.getVariable().equals(getVariable());
    }
}
