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
package org.springframework.data.neo4j.support.mapping;

import org.springframework.context.ApplicationListener;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.event.MappingContextEvent;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.index.IndexType;

/**
 * @author mh
 * @since 12.04.12
 */
public class IndexCreationMappingEventListener implements ApplicationListener<MappingContextEvent<Neo4jPersistentEntity<?>, Neo4jPersistentProperty>> {
    private Neo4jTemplate template;

    public IndexCreationMappingEventListener(Neo4jTemplate template) {
        this.template = template;
    }

    @Override
    public void onApplicationEvent(MappingContextEvent<Neo4jPersistentEntity<?>, Neo4jPersistentProperty> event) {
        if (!(event.getSource() instanceof Neo4jPersistentEntity)) return;

        final Neo4jPersistentEntity entity = event.getPersistentEntity();
        final Class entityType = entity.getType();
        template.getIndex(entityType, null, IndexType.SIMPLE);
        entity.doWithProperties(new PropertyHandler<Neo4jPersistentProperty>() {
            @Override
            public void doWithPersistentProperty(Neo4jPersistentProperty property) {
                if (property.isIndexed()) {
                    template.getIndex(property, entityType);
                }
            }
        });
    }
}
