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

import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.annotation.MapResult;
import org.springframework.data.neo4j.conversion.DefaultConverter;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.core.EntityPath;
import org.springframework.data.neo4j.mapping.EntityPersister;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.Neo4jTemplateAware;
import org.springframework.data.neo4j.support.path.ConvertingEntityPath;

import javax.inject.Provider;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * @author mh
 * @since 28.06.11
 */
public class EntityResultConverter<T, R> extends DefaultConverter<T, R> implements Neo4jTemplateAware<T,R> {
    private final ConversionService conversionService;

    public EntityResultConverter(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    private Neo4jTemplate template;
    @Override
    public ResultConverter<T,R> with(Neo4jTemplate template) {
        if (template==null) throw new IllegalArgumentException( "Template for EntityResultConverter is null" );
        this.template=template;
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
        } else
            return super.convert(value, type,mappingPolicy);
    }

}
