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
import org.springframework.data.neo4j.annotation.ResultColumn;
import org.springframework.data.neo4j.conversion.DefaultConverter;
import org.springframework.data.neo4j.core.EntityPath;
import org.springframework.data.neo4j.mapping.EntityPersister;
import org.springframework.data.neo4j.support.path.ConvertingEntityPath;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * @author mh
 * @since 28.06.11
 */
public class EntityResultConverter<T, R> extends DefaultConverter<T, R> {
    private final ConversionService conversionService;
    private final EntityPersister entityPersister;

    public EntityResultConverter(ConversionService conversionService, EntityPersister entityPersister) {
        this.conversionService = conversionService;
        this.entityPersister = entityPersister;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object doConvert(Object value, Class<?> sourceType, Class targetType) {
        if (EntityPath.class.isAssignableFrom(targetType)) {
            return new ConvertingEntityPath(entityPersister, toPath(value, sourceType));
        }
        if (entityPersister.isNodeEntity(targetType)) {
            return entityPersister.projectTo(toNode(value, sourceType), targetType);
        }
        if (entityPersister.isRelationshipEntity(targetType)) {
            return entityPersister.projectTo(toRelationship(value, sourceType), targetType);
        }
        final Object result = super.doConvert(value, sourceType, targetType);

        if (result != null) return result;

        if (conversionService.canConvert(sourceType, targetType)) {
            return conversionService.convert(value, targetType);
        }
        return result;
    }

    public R extractMapResult(Object value, Class returnType) {
        if (!Map.class.isAssignableFrom(value.getClass())) {
            throw new RuntimeException("MapResult can only be extracted from Map<String,Object>.");
        }

        InvocationHandler apa = new QueryResultProxy((Map<String, Object>) value);

        R resultProxy = (R) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{returnType}, apa);
        return resultProxy;
    }

    @Override
    public R convert(Object value, Class type) {
        if (type.isAnnotationPresent(MapResult.class)) {
            return extractMapResult(value, type);
        } else
            return super.convert(value, type);    //To change body of overridden methods use File | Settings | File Templates.
    }

    private class QueryResultProxy implements InvocationHandler {
        private final Map<String, Object> map;

        private QueryResultProxy(Map<String,Object> map) {
            this.map = map;
        }

        @Override
        public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
            ResultColumn column = method.getAnnotation(ResultColumn.class);
            TypeInformation<Object> returnType = ClassTypeInformation.fromReturnTypeOf(method);
            Object columnValue = map.get(column.value());

            Object result;
            if (returnType.isCollectionLike())
                throw new RuntimeException("apa");
//                    result = template.convert((Iterable) columnValue).to(returnType.getActualType().getType());
            else
                result = convert(columnValue, returnType.getType());


            return result;
        }

    }

}
