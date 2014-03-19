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

import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.mapping.Association;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.model.AnnotationBasedPersistentProperty;
import org.springframework.data.mapping.model.BeanWrapper;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.GraphProperty;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.RelationshipType;
import org.springframework.data.neo4j.annotation.StartNode;
import org.springframework.data.neo4j.mapping.IndexInfo;
import org.springframework.data.neo4j.mapping.ManagedEntity;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.mapping.RelationshipInfo;
import org.springframework.data.neo4j.support.DoReturn;
import org.springframework.data.util.TypeInformation;

/**
 * Implementation of {@link org.springframework.data.neo4j.mapping.Neo4jPersistentProperty}.
 *
 * @author Oliver Gierke
 */
class Neo4jPersistentPropertyImpl extends AnnotationBasedPersistentProperty<Neo4jPersistentProperty> implements
        Neo4jPersistentProperty {

    private final static Logger log = LoggerFactory.getLogger(Neo4jPersistentProperty.class);

    private final RelationshipInfo relationshipInfo;
    private final boolean isIdProperty;
    private IndexInfo indexInfo;
    private Association<Neo4jPersistentProperty> myAssociation;
    private String defaultValue;
    private Class<?> propertyType;
    private String query;
    private final boolean isNeo4jEntityType;
    private Boolean isAssociation;
    private final String neo4jPropertyName;
    private final int hash;

    public Neo4jPersistentPropertyImpl(Field field, PropertyDescriptor propertyDescriptor,
                                       PersistentEntity<?, Neo4jPersistentProperty> owner, SimpleTypeHolder simpleTypeHolder, Neo4jMappingContext ctx) {
        super(field, propertyDescriptor, owner, simpleTypeHolder);
        this.hash = field == null ? propertyDescriptor.hashCode() : field.hashCode();
        this.relationshipInfo = extractRelationshipInfo(field, ctx);
        this.propertyType = extractPropertyType();
        this.isNeo4jEntityType = isNeo4jPropertyType(getType());
        this.neo4jPropertyName = createNeo4jPropertyName();
        this.indexInfo = extractIndexInfo();
        this.isIdProperty = super.isIdProperty() || getAnnotation(GraphId.class) != null;
        this.defaultValue = extractDefaultValue();
        this.myAssociation = isAssociation() ? super.getAssociation() == null ? createAssociation() : super.getAssociation() : null;
        this.query = extractQuery();
    }

    private String extractQuery() {
        final Query query = getAnnotation(Query.class);
        if (query == null) return null;
        String value = query.value();
        return value.trim().isEmpty() ? null : value;
    }

    private Class<?> extractPropertyType() {
        final GraphProperty graphProperty = getAnnotation(GraphProperty.class);
        if (graphProperty==null) return String.class;
        return graphProperty.propertyType();
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

    private IndexInfo extractIndexInfo() {
        final Indexed annotation = getAnnotation(Indexed.class);
        return annotation!=null ? new IndexInfo(annotation,this) : null;
    }

    public <T extends Annotation> T getAnnotation(Class<? extends T> annotationType) {
        return findAnnotation(annotationType);
    }

    private RelationshipInfo extractRelationshipInfo(final Field field, Neo4jMappingContext ctx) {
        if (isAnnotationPresent(RelatedTo.class)) {
            return RelationshipInfo.fromField(getName(), getAnnotation(RelatedTo.class), getTypeInformation(), ctx);
        }

        if (isAnnotationPresent(RelatedToVia.class)) {
            return RelationshipInfo.fromField(getName(), getAnnotation(RelatedToVia.class), getTypeInformation(),ctx);
        }
        if (hasAnnotation(getTypeInformation(), NodeEntity.class)) {
            return RelationshipInfo.fromField(getName(), getTypeInformation(), ctx);
        }
        return null;
    }


    @Override
    public void setValue(Object entity, Object newValue) {
    	
    	BeanWrapper<Object> wrapper = BeanWrapper.create(entity, null);
    	wrapper.setProperty(this, newValue);
    }

    private static boolean hasAnnotation(TypeInformation<?> typeInformation, final Class<NodeEntity> annotationClass) {
        return typeInformation.getActualType().getType().isAnnotationPresent(annotationClass);
    }

    @Override
    protected Association<Neo4jPersistentProperty> createAssociation() {
        return new Association<Neo4jPersistentProperty>(this, null);
    }

    @Override
    public boolean isAssociation() {
    	
    	if (this.isAssociation == null) {
    		this.isAssociation = super.isAssociation();
    	}
    	
    	return this.isAssociation || isRelationship();
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
        return neo4jPropertyName;
    }

    private String createNeo4jPropertyName() {
        final Neo4jPersistentEntity<?> entityClass = (Neo4jPersistentEntity<?>) getOwner();
        if (entityClass.useShortNames()) return getName();
        return String.format("%s.%s", entityClass.getType().getSimpleName(), getName());
    }


    public boolean isSerializablePropertyField(final ConversionService conversionService) {
        if (isRelationship()) return false;
        final Class<?> type = getType();
        if (getTypeInformation().isCollectionLike()) {
            return isConvertible(conversionService, getComponentType());
        }
        return isConvertible(conversionService, type);
    }

    private boolean isConvertible(ConversionService conversionService, Class<?> type) {
        return conversionService.canConvert(type, propertyType) && conversionService.canConvert(propertyType, type);
    }

    @Override
    public boolean isNeo4jPropertyType() {
        return isNeo4jEntityType;
    }

    public static boolean isNumeric(final Class<?> fieldType) {
        return (fieldType.isPrimitive() && !fieldType.equals(boolean.class) && !fieldType.equals(void.class)) 
            || fieldType.equals(Character.class)
            || (fieldType.getName().startsWith("java.lang") && Number.class.isAssignableFrom(fieldType));
    }

    public boolean isIndexedNumerically() {
        if (!isIndexed() || !getIndexInfo().isNumeric()) return false;
        return isNumeric(getType()) || isNumeric(getPropertyType()) || 
               (getType().isArray() && !getType().getComponentType().isArray() && isNumeric(getType().getComponentType()));
    }

    private static boolean isNeo4jPropertyType(final Class<?> fieldType) {
        // todo: add array support
        return fieldType.equals(String.class)
                || fieldType.equals(Boolean.class)
                || fieldType.equals(boolean.class)
                || isNumeric(fieldType)
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

    public Object getValue(final Object entity, final MappingPolicy mappingPolicy) {
        if (entity instanceof ManagedEntity && !mappingPolicy.accessField()) {
            return DoReturn.unwrap(((ManagedEntity<?,?>) entity).getEntityState().getValue(this, mappingPolicy));
        }
        return getValueFromEntity(entity, mappingPolicy);
    }

    @Override
    public Object getValueFromEntity(Object entity, final MappingPolicy mappingPolicy) {
    	
    	BeanWrapper<Object> wrapper = BeanWrapper.create(entity, null);
    	return wrapper.getProperty(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getDefaultValue(ConversionService conversionService, final Class<T> targetType) {
        if (defaultValue == null) return (T) getDefaultValue(targetType);
        if (targetType.isAssignableFrom(String.class)) return (T) defaultValue;
        if (conversionService == null) return (T) getDefaultValue(targetType);
        return conversionService.convert(defaultValue, targetType);
    }

    @Override
    public Class<?> getPropertyType() {
        return propertyType;
    }

    public boolean isUnique() {
        return isIndexed() && getIndexInfo().isUnique();
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
        return (Neo4jPersistentEntity<?>) super.getOwner();
    }
    @Override
    public boolean isEntity() {
        return super.isEntity() && (isRelationshipEntity(getType()) || isNodeEntity(getType()));
    }

    private boolean isRelationshipEntity(final Class<?> type) {
        return type.isAnnotationPresent(RelationshipEntity.class);
    }

    private boolean isNodeEntity(Class<?> type) {
        return type.isAnnotationPresent(NodeEntity.class);
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

    public boolean isTransient() {
    	
    	if (super.isTransient()) {
    		return true;
    	}
    	
    	return field == null ? false : Modifier.isTransient(field.getModifiers());
    }

    @Override
    public Iterable<? extends TypeInformation<?>> getPersistentEntityType() {
        final Iterable<? extends TypeInformation<?>> result = super.getPersistentEntityType();
        for (Iterator<? extends TypeInformation<?>> it = result.iterator(); it.hasNext(); ) {
            final TypeInformation<?> typeInformation = it.next();
            final Class<?> type = typeInformation.getType();
            if (isNodeEntity(type) || isRelationshipEntity(type)) continue;
            if (log.isInfoEnabled()) log.info("ignoring "+getName()+" "+type+" "+typeInformation.getActualType().getType());
            it.remove();
        }
        return result;
    }

    public MappingPolicy obtainMappingPolicy(MappingPolicy providedMappingPolicy) {
        if (providedMappingPolicy != null) return providedMappingPolicy;
        return getMappingPolicy();
    }
 
    public int hashCode() {
        return hash;
    }

    public boolean hasQuery() {
        return this.query!=null;
    }

    public String getQuery() {
        return query;
    }

    @Override
    public Class<?> getTargetType() {
        if (! isTargetTypeEnforced()) return null;

        if (isCollectionLike()) return getComponentType();

        return getType();
    }

    @Override
    public boolean isTargetTypeEnforced() {
        return getAnnotation( RelatedTo.class ) != null && getAnnotation( RelatedTo.class ).enforceTargetType();
    }
    
	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.neo4j.mapping.Neo4jPersistentProperty#isStartNode()
	 */
	@Override
	public boolean isStartNode() {
		return isAnnotationPresent(StartNode.class);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.neo4j.mapping.Neo4jPersistentProperty#isEndNode()
	 */
	@Override
	public boolean isEndNode() {
		return isAnnotationPresent(EndNode.class);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.neo4j.mapping.Neo4jPersistentProperty#isRelationshipType()
	 */
	@Override
	public boolean isRelationshipType() {
		return isAnnotationPresent(RelationshipType.class);
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.mapping.model.AnnotationBasedPersistentProperty#isIdProperty()
	 */
	@Override
	public boolean isIdProperty() {
		return isIdProperty;
	}
}
