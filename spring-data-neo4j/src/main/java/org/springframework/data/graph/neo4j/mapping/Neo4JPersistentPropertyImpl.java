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

package org.springframework.data.graph.neo4j.mapping;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;

import org.springframework.data.graph.annotation.GraphId;
import org.springframework.data.graph.annotation.RelatedTo;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AbstractPersistentProperty;
import org.springframework.data.mapping.model.SimpleTypeHolder;

/**
 * Implementation of {@link Neo4JPersistentProperty}.
 * 
 * @author Oliver Gierke
 */
class Neo4JPersistentPropertyImpl extends AbstractPersistentProperty<Neo4JPersistentProperty> implements
Neo4JPersistentProperty {

    private final RelationshipInfo relationshipInfo;
    private final boolean isIdProperty;

    /**
     * Creates a new {@link Neo4JPersistentPropertyImpl} from the given {@link Field}, {@link PropertyDescriptor} owner
     * {@link PersistentEntity} and {@link SimpleTypeHolder}.
     * 
     * @param field
     * @param propertyDescriptor
     * @param owner
     * @param simpleTypeHolder
     */
    public Neo4JPersistentPropertyImpl(Field field, PropertyDescriptor propertyDescriptor,
            PersistentEntity<?, Neo4JPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {
        super(field, propertyDescriptor, owner, simpleTypeHolder);

        this.relationshipInfo = field.isAnnotationPresent(RelatedTo.class) ? new AnnotationBasedRelationshipInfo(
                field.getAnnotation(RelatedTo.class)) : null;
                this.isIdProperty = field.isAnnotationPresent(GraphId.class);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.mapping.PersistentProperty#isIdProperty()
     */
    @Override
    public boolean isIdProperty() {
        return this.isIdProperty;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.mapping.model.AbstractPersistentProperty#createAssociation()
     */
    @Override
    protected Association<Neo4JPersistentProperty> createAssociation() {
        return new Association<Neo4JPersistentProperty>(this, null);
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.graph.neo4j.mapping.Neo4JPersistentProperty#isRelationship()
     */
    @Override
    public boolean isRelationship() {
        return this.relationshipInfo != null;
    }

    /*
     * (non-Javadoc)
     * @see org.springframework.data.graph.neo4j.mapping.Neo4JPersistentProperty#getRelationShipInfo()
     */
    @Override
    public RelationshipInfo getRelationshipInfo() {
        return relationshipInfo;
    }
}
