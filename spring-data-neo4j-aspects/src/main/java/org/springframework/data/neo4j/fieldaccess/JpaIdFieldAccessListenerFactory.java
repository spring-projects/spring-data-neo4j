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

package org.springframework.data.neo4j.fieldaccess;

import org.springframework.data.neo4j.core.EntityState;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

import javax.persistence.Id;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
public class JpaIdFieldAccessListenerFactory implements FieldAccessorListenerFactory {
    private final GraphDatabaseContext graphDatabaseContext;

    public JpaIdFieldAccessListenerFactory(GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }

    @Override
    public boolean accept(final Neo4jPersistentProperty property) {
        return property.isAnnotationPresent(Id.class);
    }

    @Override
    public FieldAccessListener forField(final Neo4jPersistentProperty property) {
        return new JpaIdFieldListener(property,graphDatabaseContext);
    }

    public static class JpaIdFieldListener implements FieldAccessListener {
        protected final Neo4jPersistentProperty property;
        private final GraphDatabaseContext graphDatabaseContext;

        public JpaIdFieldListener(final Neo4jPersistentProperty property, GraphDatabaseContext graphDatabaseContext) {
            this.property = property;
            this.graphDatabaseContext = graphDatabaseContext;
        }

        @Override
        public void valueChanged(Object entity, Object oldVal, Object newVal) {
            if (newVal != null) {
                graphDatabaseContext.save(entity);
/* TODO                EntityState entityState = entity.getEntityState();
                entityState.persist();
*/
            }
        }
    }
}
