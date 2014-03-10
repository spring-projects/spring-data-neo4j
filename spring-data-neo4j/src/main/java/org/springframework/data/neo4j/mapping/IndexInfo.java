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
package org.springframework.data.neo4j.mapping;

import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.support.index.IndexType;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;

/**
* @author mh
* @since 18.10.11
*/
public class IndexInfo {
    private String indexName;
    private IndexType indexType;
    private final String fieldName;
    private final Indexed.Level level;
    private String indexKey;
    private final boolean unique;
    private boolean numeric;
    private Indexed annotation;
    private Neo4jPersistentProperty property;

    public IndexInfo(Indexed annotation,
                     Neo4jPersistentProperty property) {
        this.annotation = annotation;
        this.property = property;
        this.indexType = annotation.indexType();
        fieldName = annotation.fieldName();
        this.indexKey = fieldName.isEmpty() ? property.getNeo4jPropertyName() : fieldName;
        unique = annotation.unique();
        level = annotation.level();
        numeric = annotation.numeric();
    }

    private String determineLabelIndexName(Indexed annotation, Neo4jPersistentProperty property) {
        if (!annotation.indexName().isEmpty()) throw new MappingException("No index name allowed on label based indexes, property: "+ property.getOwner().getName()+"."+property.getName());
        Neo4jPersistentEntity<?> entity = property.getOwner();

        // NW StoredEntityType not available at this stage yet ....
        // only set when entity.updateStoredType(..) called
        StoredEntityType entityType = entity.getEntityType();
        switch (annotation.level()) {
            case CLASS:
                Class<?> declaringClass = property.getField().getDeclaringClass();
                return
                    (entityType != null)
                            ? entityType.findByTypeClass(declaringClass).getAlias().toString()
                            : doBestGuessLabelName(entity);  // Not right but not sure what to do here
            case INSTANCE:
                return
                    (entityType != null)
                            ? entityType.getAlias().toString()
                            : doBestGuessLabelName(entity);  // Not right but not sure what to do here
            case GLOBAL: throw new MappingException("No global index for label based indexes");
        }
        return entityType.getAlias().toString();
    }

    private String doBestGuessLabelName(Neo4jPersistentEntity<?> entity) {
        if (entity.getTypeAlias() != null) {
            return (String)entity.getTypeAlias();
        }
        return entity.getType().getSimpleName();
    }


    private String determineIndexName(Indexed annotation, Neo4jPersistentProperty property) {
        final String providedIndexName = annotation.indexName().isEmpty() ? null : annotation.indexName();
        final Class<?> declaringClass = property.getField().getDeclaringClass();
        final Class<?> instanceType = property.getOwner().getType();
        return Indexed.Name.get(annotation.level(), declaringClass, providedIndexName, instanceType);
    }

    public boolean isLabelBased() {
        return indexType.isLabelBased();
    }

    // lazy because of deferred persistent entity hierarchy determination on registration
    public String getIndexName() {
        if (indexName == null) {
            this.indexName = isLabelBased() ? determineLabelIndexName(annotation, property) : determineIndexName(annotation, property);
        }
        return indexName;
    }

    public IndexType getIndexType() {
        return indexType;
    }

    public boolean isFullText() {
        return indexType == IndexType.FULLTEXT;
    }

    public String getIndexKey() {
        return indexKey;
    }

    public boolean isUnique() {
        return unique;
    }

    public boolean isNumeric() {
        return numeric;
    }
}
