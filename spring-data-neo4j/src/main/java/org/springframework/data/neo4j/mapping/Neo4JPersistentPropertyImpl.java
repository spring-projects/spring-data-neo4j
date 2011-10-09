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

import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AbstractPersistentProperty;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.neo4j.annotation.*;


import org.springframework.data.util.TypeInformation;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Implementation of {@link Neo4jPersistentProperty}.
 *
 * @author Oliver Gierke
 */
class Neo4jPersistentPropertyImpl extends AbstractPersistentProperty<Neo4jPersistentProperty> implements
        Neo4jPersistentProperty {

    private final RelationshipInfo relationshipInfo;
    private final boolean isIdProperty;
    private IndexInfo indexInfo;
    private Map<Class<? extends Annotation>, ? extends Annotation> annotations;

    public Neo4jPersistentPropertyImpl(Field field, PropertyDescriptor propertyDescriptor,
                                       PersistentEntity<?, Neo4jPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder) {
        super(field, propertyDescriptor, owner, simpleTypeHolder);
        this.annotations = extractAnnotations(field);
        this.relationshipInfo = extractRelationshipInfo(field);
        this.indexInfo = extractIndexInfo();
        this.isIdProperty = annotations.containsKey(GraphId.class);
    }

    private Map<Class<? extends Annotation>,? extends Annotation> extractAnnotations(Field field) {
        Map<Class<? extends Annotation>, Annotation> result=new IdentityHashMap<Class<? extends Annotation>, Annotation>();
        for (Annotation annotation : field.getAnnotations()) {
            result.put(annotation.annotationType(), annotation);
        }
        return result;
    }

    private IndexInfo extractIndexInfo() {
        final Indexed annotation = getAnnotation(Indexed.class);
        return annotation!=null ? new IndexInfo(annotation,this) : null;
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        return (T) annotations.get(annotationType);
    }

    private RelationshipInfo extractRelationshipInfo(final Field field) {
        if (isAnnotationPresent(RelatedTo.class)) {
            return RelationshipInfo.fromField(field, getAnnotation(RelatedTo.class), getTypeInformation());
        }

        if (isAnnotationPresent(RelatedToVia.class)) {
            return RelationshipInfo.fromField(field, getAnnotation(RelatedToVia.class), getTypeInformation());
        }
        if (hasAnnotation(getTypeInformation(), NodeEntity.class)) {
            return RelationshipInfo.fromField(field, getTypeInformation());
        }
        return null;
    }

    public <T extends Annotation> boolean isAnnotationPresent(Class<T> annotationType) {
        return annotations.containsKey(annotationType);
    }

    @Override
    public void setValue(Object entity, Object newValue) {
        try {
            if (!field.isAccessible()) field.setAccessible(true);
            field.set(entity, newValue);
        } catch (IllegalAccessException e) {
            throw new MappingException("Could not access field "+field+" for setting value "+newValue+" on "+this);
        }
    }

    private static boolean hasAnnotation(TypeInformation<?> typeInformation, final Class<NodeEntity> annotationClass) {
        return typeInformation.getActualType().getType().isAnnotationPresent(annotationClass);
    }

    @Override
    public boolean isIdProperty() {
        return this.isIdProperty;
    }

    @Override
    protected Association<Neo4jPersistentProperty> createAssociation() {
        return new Association<Neo4jPersistentProperty>(this, null);
    }

    @Override
    public boolean isRelationship() {
        return this.relationshipInfo != null;
    }

    @Override
    public RelationshipInfo getRelationshipInfo() {
        return relationshipInfo;
    }

    @Override
    public boolean isIndexed() {
        return indexInfo != null;
    }

    @Override
    public IndexInfo getIndexInfo() {
        return indexInfo;
    }

    public String getNeo4jPropertyName() {
        final Neo4jPersistentEntity entityClass = (Neo4jPersistentEntity) getOwner();
        if (entityClass.useShortNames()) return getName();
        return String.format("%s.%s", entityClass.getType().getSimpleName(), getName());
    }


    public boolean isSimpleValueField() {
        return !(getTypeInformation().isCollectionLike() || isRelationship());
    }

    public boolean isSerializableField(final ConversionService conversionService) {
        return isSimpleValueField() && conversionService.canConvert(getType(), String.class);
    }

    public boolean isDeserializableField(final ConversionService conversionService) {
        return isSimpleValueField() && conversionService.canConvert(String.class, getType());
    }

    @Override
    public boolean isNeo4jPropertyType() {
        return isNeo4jPropertyType(getType());
    }

    private static boolean isNeo4jPropertyType(final Class<?> fieldType) {
        // todo: add array support
        return fieldType.isPrimitive()
                || fieldType.equals(String.class)
                || fieldType.equals(Character.class)
                || fieldType.equals(Boolean.class)
                || (fieldType.getName().startsWith("java.lang") && Number.class.isAssignableFrom(fieldType))
                || (fieldType.isArray() && !fieldType.getComponentType().isArray() && isNeo4jPropertyType(fieldType.getComponentType()));
    }

    public boolean isSyntheticField() {
        return getName().contains("$");
    }

    @Override
    public Collection<? extends Annotation> getAnnotations() {
        return annotations.values();
    }

    public Object getValue(final Object entity) {
        try {
            final Field field = getField();
            if (!field.isAccessible()) field.setAccessible(true);
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new MappingException("Could not access field "+field);
        }
    }


    public static class IndexInfo {
        private String indexName;
        private boolean fulltext;
        private final String fieldName;
        private final Indexed.Level level;

        public IndexInfo(Indexed annotation, Neo4jPersistentPropertyImpl property) {
            this.indexName = determineIndexName(annotation,property);
            this.fulltext = annotation.fulltext();
            fieldName = annotation.fieldName();
            level = annotation.level();
        }


        private String determineIndexName(Indexed annotation, Neo4jPersistentPropertyImpl property) {
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

    @Override
    public String toString() {
        return getType() +" "+ getName() + " rel: "+isRelationship()+ " idx: "+isIndexed();
    }

    @Override
    public Neo4jPersistentEntity<?> getOwner() {
        return (Neo4jPersistentEntity<?>)super.getOwner();
    }
    @Override
    public boolean isEntity() {
        return super.isEntity() && (isRelationshipEntity() || isNodeEntity());
    }

    private boolean isRelationshipEntity() {
        return getType().isAnnotationPresent(RelationshipEntity.class);
    }
    private boolean isNodeEntity() {
        return getType().isAnnotationPresent(NodeEntity.class);
    }
}
