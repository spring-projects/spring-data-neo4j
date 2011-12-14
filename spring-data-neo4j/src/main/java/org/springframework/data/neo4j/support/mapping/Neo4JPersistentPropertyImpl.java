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

import org.springframework.core.convert.ConversionService;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AbstractPersistentProperty;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.mapping.*;
import org.springframework.data.neo4j.support.DoReturn;
import org.springframework.data.util.TypeInformation;

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Implementation of {@link org.springframework.data.neo4j.mapping.Neo4jPersistentProperty}.
 *
 * @author Oliver Gierke
 */
class Neo4jPersistentPropertyImpl extends AbstractPersistentProperty<Neo4jPersistentProperty> implements
        Neo4jPersistentProperty {

    private final RelationshipInfo relationshipInfo;
    private final boolean isIdProperty;
    private IndexInfo indexInfo;
    private Map<Class<? extends Annotation>, ? extends Annotation> annotations;
    private Association<Neo4jPersistentProperty> myAssociation;
    private String defaultValue;

    public Neo4jPersistentPropertyImpl(Field field, PropertyDescriptor propertyDescriptor,
                                       PersistentEntity<?, Neo4jPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder, Neo4jMappingContext ctx) {
        super(field, propertyDescriptor, owner, simpleTypeHolder);
        this.annotations = extractAnnotations(field);
        this.relationshipInfo = extractRelationshipInfo(field, ctx);
        this.indexInfo = extractIndexInfo();
        this.isIdProperty = annotations.containsKey(GraphId.class);
        this.defaultValue = extractDefaultValue();
        this.myAssociation = isAssociation() ? super.getAssociation() == null ? createAssociation() : super.getAssociation() : null;
    }

    private String extractDefaultValue() {
        final GraphProperty graphProperty = getAnnotation(GraphProperty.class);
        if (graphProperty==null) return null;
        final String value = graphProperty.defaultValue();
        if (value.equals(GraphProperty.UNSET_DEFAULT)) return null;
        return value;
    }

    @Override
    public Association<Neo4jPersistentProperty> getAssociation() {
        return myAssociation;
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

    private RelationshipInfo extractRelationshipInfo(final Field field, Neo4jMappingContext ctx) {
        if (isAnnotationPresent(RelatedTo.class)) {
            return RelationshipInfo.fromField(field, getAnnotation(RelatedTo.class), getTypeInformation(), ctx);
        }

        if (isAnnotationPresent(RelatedToVia.class)) {
            return RelationshipInfo.fromField(field, getAnnotation(RelatedToVia.class), getTypeInformation(),ctx);
        }
        if (hasAnnotation(getTypeInformation(), NodeEntity.class)) {
            return RelationshipInfo.fromField(field, getTypeInformation(), ctx);
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
    public boolean isAssociation() {
        return super.isAssociation() || isRelationship();
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


    public boolean isSerializablePropertyField(final ConversionService conversionService) {
        if (isRelationship()) return false;
        final Class<?> type = getType();
        final Class<String> targetType = String.class;
        if (getTypeInformation().isCollectionLike()) {
            return isConvertibleBetween(conversionService, getComponentType(), targetType);
        }
        return isConvertibleBetween(conversionService, type, targetType);
    }

    private boolean isConvertibleBetween(ConversionService conversionService, Class<?> type, Class<String> targetType) {
        return conversionService.canConvert(type, targetType) && conversionService.canConvert(targetType, type);
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

    @Override
    public boolean isNeo4jPropertyValue(Object value) {
	if (value == null || value.getClass().isArray()) {
	    return false;
	}
	return isNeo4jPropertyType(value.getClass());
    }

    public boolean isSyntheticField() {
        return getName().contains("$");
    }

    @Override
    public Collection<? extends Annotation> getAnnotations() {
        return annotations.values();
    }

    public Object getValue(final Object entity, final MappingPolicy mappingPolicy) {
        if (entity instanceof ManagedEntity && !mappingPolicy.accessField()) {
            return DoReturn.unwrap(((ManagedEntity) entity).getEntityState().getValue(this, mappingPolicy));
        }
        return getValueFromEntity(entity, mappingPolicy);
    }

    @Override
    public Object getValueFromEntity(Object entity, final MappingPolicy mappingPolicy) {
        try {
            final Field field = getField();
            if (!field.isAccessible()) field.setAccessible(true);
            return field.get(entity);
        } catch (IllegalAccessException e) {
            throw new MappingException("Could not access field "+field);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getDefaultValue(ConversionService conversionService, final Class<T> targetType) {
        if (defaultValue == null) return (T) getDefaultValue(targetType);
        if (targetType.isAssignableFrom(String.class)) return (T) defaultValue;
        if (conversionService == null) return (T) getDefaultValue(targetType);
        return conversionService.convert(defaultValue, targetType);
    }

    private Object getDefaultValue(Class<?> type) {
        if (type!=null && type.isPrimitive()) {
            if (type.equals(boolean.class)) return false;
            return 0;
        }
        return null;
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
        return super.isEntity() && (hasRelationshipEntityType() || hasNodeEntityType());
    }

    private boolean hasRelationshipEntityType() {
        return getType().isAnnotationPresent(RelationshipEntity.class);
    }
    private boolean hasNodeEntityType() {
        return getType().isAnnotationPresent(NodeEntity.class);
    }

    public String getIndexKey() {
        return getIndexInfo().getIndexKey();
    }

    @Override
    public MappingPolicy getMappingPolicy() {
        if (isAnnotationPresent(Fetch.class))
            return MappingPolicy.LOAD_POLICY;
        else
            return MappingPolicy.DEFAULT_POLICY;
    }

    /**
     * @deprecated todo remove when SD-Commons handles transient properties differently
     */
    public boolean isReallyTransient() {
   		return Modifier.isTransient(field.getModifiers()) || isAnnotationPresent(Transient.class) || isAnnotationPresent("javax.persistence.Transient");
   	}

    private boolean isAnnotationPresent(String className) {
        for (Class<? extends Annotation> annotationType : annotations.keySet()) {
            if (annotationType.getName().equals(className)) return true;
        }
        return false;
    }

    @Override
    public boolean isTransient() {
        return false;
    }
}
