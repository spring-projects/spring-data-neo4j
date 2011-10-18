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

import org.springframework.data.neo4j.annotation.Indexed;

/**
* @author mh
* @since 18.10.11
*/
public class IndexInfo {
    private String indexName;
    private boolean fulltext;
    private final String fieldName;
    private final Indexed.Level level;

    public IndexInfo(Indexed annotation, Neo4jPersistentProperty property) {
        this.indexName = determineIndexName(annotation,property);
        this.fulltext = annotation.fulltext();
        fieldName = annotation.fieldName();
        level = annotation.level();
    }


    private String determineIndexName(Indexed annotation, Neo4jPersistentProperty property) {
        final String providedIndexName = annotation.indexName().isEmpty() ? null : annotation.indexName();
        final Class<?> declaringClass = property.getField().getDeclaringClass();
        final Class<?> instanceType = property.getOwner().getType();
        return Indexed.Name.get(annotation.level(), declaringClass, providedIndexName, instanceType);
    }

    public String getIndexName() {
        return indexName;
    }

    public boolean isFulltext() {
        return fulltext;
    }
}
