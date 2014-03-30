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
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.util.Assert;

/**
 * This class represents ....
 * All the information about a particular part of a Neo4jPersistentProperty?
 * @author mh
 * @since 31.10.11
 */
public class PartInfo {
    private final PersistentPropertyPath<Neo4jPersistentProperty> path;
    private final String identifier;
    private final Part part;
    private final int index;

    public PartInfo(PersistentPropertyPath<Neo4jPersistentProperty> path, String identifier, Part part, int index) {
        Assert.notNull(path);
        Assert.hasText(identifier);

        this.path = path;
        this.identifier = identifier;
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

    public boolean isLabelIndexed() {
        return isIndexed() && getLeafProperty().getIndexInfo().isLabelBased();
    }

    public boolean isIndexed() {
        return getLeafProperty().isIndexed();
    }

    public String getIdentifier() {
        return identifier;
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
    public boolean isSpatial() {
        return isIndexed() && getIndexInfo().getIndexType() == IndexType.POINT;
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

    boolean sameIdentifier(PartInfo startPartInfo) {
        return startPartInfo.getIdentifier().equals(getIdentifier());
    }
}
