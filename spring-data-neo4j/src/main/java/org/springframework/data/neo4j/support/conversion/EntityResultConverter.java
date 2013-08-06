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

package org.springframework.data.neo4j.support.conversion;

import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.annotation.MapResult;
import org.springframework.data.neo4j.annotation.POJOResult;
import org.springframework.data.neo4j.annotation.ResultColumn;
import org.springframework.data.neo4j.conversion.DefaultConverter;
import org.springframework.data.neo4j.conversion.QueryResultBuilder;
import org.springframework.data.neo4j.core.EntityPath;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.Neo4jTemplateAware;
import org.springframework.data.neo4j.support.path.ConvertingEntityPath;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * @author mh
 * @since 28.06.11
 */
public class EntityResultConverter<T, R> extends DefaultConverter<T, R> implements Neo4jTemplateAware<EntityResultConverter<T,R>> {
    private final ConversionService conversionService;
    private Neo4jTemplate template;

    public EntityResultConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    public EntityResultConverter( ConversionService conversionService, Neo4jTemplate template )
    {
        this.conversionService = conversionService;
        this.template = template;
    }

    @Override
    public EntityResultConverter<T,R> with(Neo4jTemplate template) {
        if (template == null) throw new IllegalArgumentException( "Template for EntityResultConverter must not be null" );
        this.template = template;
        return this;
    }


    @SuppressWarnings("unchecked")
    @Override
    protected Object doConvert(Object value, Class<?> sourceType, Class targetType, MappingPolicy mappingPolicy) {
        if (EntityPath.class.isAssignableFrom(targetType)) {
            return new ConvertingEntityPath(toPath(value, sourceType),template);
        }
        if (template.isNodeEntity(targetType)) {
            return template.projectTo(toNode(value, sourceType), targetType, mappingPolicy);
        }
        if (template.isRelationshipEntity(targetType)) {
            return template.projectTo(toRelationship(value, sourceType), targetType, mappingPolicy);
        }
        final Object result = super.doConvert(value, sourceType, targetType, mappingPolicy);

        if (result != null) return result;

        if (conversionService.canConvert(sourceType, targetType)) {
            return conversionService.convert(value, targetType);
        }
        return result;
    }


    @SuppressWarnings("unchecked")
    public R extractPOJOResult(Object value, Class returnType, MappingPolicy mappingPolicy) {
        String errorMessage = "Error extracting and setting value for POJO Result : " + returnType;
        if (!Map.class.isAssignableFrom(value.getClass())) {
            throw new RuntimeException("POJOResult can only be extracted from Map<String,Object>.");
        }

        Object newThing = null;
        ResultColumnValueExtractor resultColumnValueExtractor = new ResultColumnValueExtractor((Map<String, Object>) value,mappingPolicy,this);
        try {
            newThing = returnType.newInstance();
            BeanWrapper wrapper = new BeanWrapperImpl( newThing );
            for (Field field: returnType.getDeclaredFields()) {
                extractAndSetValueOfField(wrapper, field, resultColumnValueExtractor);
            }
        } catch (IllegalAccessException e1) {
            throw new POJOResultBuildingException(errorMessage, e1);
        } catch (InstantiationException e2) {
            throw new POJOResultBuildingException(errorMessage, e2);
        } catch (InvocationTargetException e3) {
            throw new POJOResultBuildingException(errorMessage, e3);
        } catch (NoSuchMethodException e4) {
            throw new POJOResultBuildingException(errorMessage, e4);
        } catch (ClassNotFoundException e5) {
            throw new POJOResultBuildingException(errorMessage, e5);
        }

        return (R) newThing;
    }

    private void extractAndSetValueOfField(BeanWrapper wrapper, Field field,
                                           ResultColumnValueExtractor resultColumnValueExtractor)
        throws InvocationTargetException , NoSuchMethodException , ClassNotFoundException , IllegalAccessException{
        if (!isPOJOMappableField(field))
            return;

        Object val = resultColumnValueExtractor.extractFromField(field);
        if (val != null) {
            if (val.getClass().getEnclosingClass() != null &&
                val.getClass().getEnclosingClass().equals(QueryResultBuilder.class)) {
                val = IteratorUtil.asCollection((Iterable) val);
            }
            wrapper.setPropertyValue( field.getName(), val );
        }

    }

    /**
     * At present, the only fields which can be mapped to a POJO are those
     * annotated with the ResultColumn annotation
     *
     * @param field
     * @return
     */
    private boolean isPOJOMappableField(Field field) {
        return field.getAnnotation(ResultColumn.class) != null;
    }

    @SuppressWarnings("unchecked")
    public R extractMapResult(Object value, Class returnType, MappingPolicy mappingPolicy) {
        if (!Map.class.isAssignableFrom(value.getClass())) {
            throw new RuntimeException("MapResult can only be extracted from Map<String,Object>.");
        }

        InvocationHandler handler = new QueryResultProxy((Map<String, Object>) value,mappingPolicy,this);

        return (R) Proxy.newProxyInstance(returnType.getClassLoader(), new Class[]{returnType}, handler);
    }

    @Override
    public R convert(Object value, Class type, MappingPolicy mappingPolicy) {
        if (type.isAnnotationPresent(MapResult.class)) {
            return extractMapResult(value, type,mappingPolicy);
        } else if (type.isAnnotationPresent(POJOResult.class)) {
            return extractPOJOResult(value, type,mappingPolicy);
        } else
            return super.convert(value, type,mappingPolicy);
    }

}
