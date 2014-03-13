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

import java.lang.annotation.Annotation;
import java.util.*;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentProperty;
import org.springframework.data.mapping.PropertyHandler;
import org.springframework.data.mapping.SimplePropertyHandler;
import org.springframework.data.mapping.model.BasicPersistentEntity;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.mapping.*;
import org.springframework.data.neo4j.support.typerepresentation.LabelBasedNodeTypeRepresentationStrategy;
import org.springframework.data.util.TypeInformation;

/**
 * Implementation of {@link org.springframework.data.neo4j.mapping.Neo4jPersistentEntity}.
 *
 * @author Oliver Gierke
 */
public class Neo4jPersistentEntityImpl<T> extends BasicPersistentEntity<T, Neo4jPersistentProperty> implements Neo4jPersistentEntity<T>, RelationshipProperties {

    private Map<Class<? extends Annotation>,Annotation> annotations=new IdentityHashMap<Class<? extends Annotation>,Annotation>();
    private final boolean managed;
    private Neo4jPersistentProperty startNodeProperty;
    private Neo4jPersistentProperty endNodeProperty;
    private Neo4jPersistentProperty relationshipType;
    private StoredEntityType storedType;
    private Neo4jPersistentProperty uniqueProperty;
    private final boolean shouldUseShortNames;
    private final EntityAlias entityAlias;
    private Set<String> labels;

    /**
     * Creates a new {@link Neo4jPersistentEntityImpl} instance.
     *
     * @param information must not be {@literal null}.
     * @param entityAlias
     */
    public Neo4jPersistentEntityImpl(TypeInformation<T> information, EntityAlias entityAlias) {
        super(information);
        this.entityAlias = entityAlias;
        for (Annotation annotation : information.getType().getAnnotations()) {
            annotations.put(annotation.annotationType(),annotation);
        }
        managed = ManagedEntity.class.isAssignableFrom(information.getType());
        shouldUseShortNames = shouldUseShortNames();
        updateStoredType(null);
    }

    void updateStoredType(Collection<Neo4jPersistentEntity<?>> superTypeEntities) {
        this.storedType = new StoredEntityType(this,superTypeEntities,entityAlias);
        this.labels = computeLabels();
    }

    @Override
    public void verify() {
        super.verify();
        doWithProperties(new PropertyHandler<Neo4jPersistentProperty>() {
            Neo4jPersistentProperty unique = null;
            public void doWithPersistentProperty(Neo4jPersistentProperty property) {
                if (property.isUnique()) {
                    if (unique!=null) throw new MappingException("Duplicate unique property " + qualifiedPropertyName(property)+ ", " + qualifiedPropertyName(uniqueProperty) + " has already been defined. Only one unique property is allowed per type");
                    unique = property;
                }
            }
        });
        if (isManaged() || getType().isInterface()) {
            return;
        }
        final Neo4jPersistentProperty idProperty = getIdProperty();
        if (idProperty == null) throw new MappingException("No id property in " + this);
        if (idProperty.getType().isPrimitive()) throw new MappingException("The type of the id-property in " + qualifiedPropertyName(idProperty)+" must not be a primitive type but an object type like java.lang.Long");
    }

    private String qualifiedPropertyName(Neo4jPersistentProperty persistentProperty) {
        return getName() + "." + persistentProperty.getName();
    }

    public boolean useShortNames() {
        return shouldUseShortNames;
    }

    private boolean shouldUseShortNames() {
        final NodeEntity graphEntity = getAnnotation(NodeEntity.class);
        if (graphEntity != null) return graphEntity.useShortNames();
        final RelationshipEntity graphRelationship = getAnnotation(RelationshipEntity.class);
        if (graphRelationship != null) return graphRelationship.useShortNames();
        return false;
    }

    @Override
    public boolean isNodeEntity() {
        return hasAnnotation(NodeEntity.class);
    }

    @Override
    public boolean isRelationshipEntity() {
        return hasAnnotation(RelationshipEntity.class);
    }

    @SuppressWarnings("unchecked")
    private <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return (T) annotations.get(annotationType);
    }

    private <T extends Annotation> boolean hasAnnotation(Class<T> annotationType) {
        return annotations.containsKey(annotationType);
    }

    @Override
    public void setPersistentState(Object entity, PropertyContainer state) {
        final Neo4jPersistentProperty idProperty = getIdProperty();
        Object id = getStateId(state);
        idProperty.setValue(entity, id);
    }

    private Object getStateId(PropertyContainer state) {
        if (state==null) return null;
        if (isNodeEntity()) {
            return ((Node) state).getId();
        }
        if (isRelationshipEntity()) {
            return ((Relationship)state).getId();
        }
        throw new MappingException("Entity has to be annotated with @NodeEntity or @RelationshipEntity"+this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getPersistentId(Object entity) {
        final Neo4jPersistentProperty idProperty = getIdProperty();
        if (idProperty==null) throw new MappingException("No field annotated with @GraphId found in "+ getEntityName());
        return idProperty.getValue(entity, idProperty.getMappingPolicy());
    }

    @Override
    public RelationshipProperties getRelationshipProperties() {
        return isRelationshipEntity() ? this : null;
    }

    public String getEntityName() {
        return getType().getName();
    }

    public boolean isManaged() {
        return managed;
    }

    @Override
    public boolean isUnique() {
        return uniqueProperty!=null;
    }

    @Override
    public void addPersistentProperty(Neo4jPersistentProperty property) {
        super.addPersistentProperty(property);
        if (property.isRelationshipType()) {
            this.relationshipType = property;
        }
        if (property.isUnique()) {
            if (this.uniqueProperty==null) {
                this.uniqueProperty = property;
            }
        }
    }

    @Override
    public void addAssociation(Association<Neo4jPersistentProperty> neo4jPersistentPropertyAssociation) {
        super.addAssociation(neo4jPersistentPropertyAssociation);
        final Neo4jPersistentProperty property = neo4jPersistentPropertyAssociation.getInverse();
        if (property.isStartNode()) {
            this.startNodeProperty = property;
        }
        if (property.isEndNode()) {
            this.endNodeProperty = property;
        }
    }

    @Override
    public Neo4jPersistentProperty getStartNodeProperty() {
        return startNodeProperty;
    }

    @Override
    public Neo4jPersistentProperty getEndNodeProperty() {
        return endNodeProperty;
    }

    @Override
    public Neo4jPersistentProperty getTypeProperty() {
        return relationshipType;
    }

    @Override
    public String getRelationshipType() {
        if (!isRelationshipEntity()) return null;
        final RelationshipEntity annotation = getAnnotation(RelationshipEntity.class);
        return annotation.type().isEmpty() ? null : annotation.type();
    }

    @Override
    public String toString() {
        return String.format("%s %smanaged @%sEntity Annotations: %s", getType(), isManaged() ? "" : "un", isNodeEntity() ? "Node" : "Relationship", annotations.keySet());
    }

    @Override
    public MappingPolicy getMappingPolicy() {
        return MappingPolicy.LOAD_POLICY;
    }

    @Override
    public StoredEntityType getEntityType() {
        return storedType;
    }

    public boolean matchesAlias(Object alias) {
        return storedType.matchesAlias(alias);
    }

    public Neo4jPersistentProperty getUniqueProperty() {
        return uniqueProperty;
    }

    @Override
    public Collection<String> getAllLabels() {
        return labels;
    }

    private Set<String> computeLabels() {
        String alias = storedType.getAlias().toString();
        final Set<String> labels = collectSuperTypeLabels(storedType, new LinkedHashSet<String>());
        labels.addAll(computeIndexBasedLabels(this));
        labels.add(alias);
        // TODO workaround check if this MC is label based from the TRS
//        labels.add(LabelBasedNodeTypeRepresentationStrategy.LABELSTRATEGY_PREFIX+alias);
        return labels;
    }

    private Set<String> computeIndexBasedLabels(Neo4jPersistentEntity<?> entity) {
        final Set<String> labels = new LinkedHashSet<>();
        entity.doWithProperties(new PropertyHandler<Neo4jPersistentProperty>() {
            @Override
            public void doWithPersistentProperty(Neo4jPersistentProperty persistentProperty) {
                if (persistentProperty.isIndexed()) {
                    IndexInfo indexInfo = persistentProperty.getIndexInfo();
                    if (indexInfo.isLabelBased()) {
                        labels.add(indexInfo.getIndexName());
                    }
                }
            }
        });
        return labels;
    }

    private Set<String> collectSuperTypeLabels(StoredEntityType type, Set<String> labels) {
        if (type==null) return labels;
        for (StoredEntityType superType : type.getSuperTypes()) {
            labels.addAll(superType.getEntity().getAllLabels());
            labels.addAll(computeIndexBasedLabels(superType.getEntity()));
            collectSuperTypeLabels(superType, labels);
        }
        return labels;
    }
}
