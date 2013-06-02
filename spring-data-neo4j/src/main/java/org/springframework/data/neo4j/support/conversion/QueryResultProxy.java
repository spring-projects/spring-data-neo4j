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

import org.springframework.data.neo4j.annotation.ResultColumn;
import org.springframework.data.neo4j.conversion.QueryResultBuilder;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

/**
* @author mh
* @since 10.11.11
*/
public class QueryResultProxy implements InvocationHandler {
    private final Map<String, Object> map;
    private final MappingPolicy mappingPolicy;
    private final ResultConverter converter;

    public QueryResultProxy(Map<String, Object> map, MappingPolicy mappingPolicy, ResultConverter converter) {
        this.map = map;
        this.mappingPolicy = mappingPolicy;
        this.converter = converter;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
        if (method.getName().equals("equals") && params!=null && params.length == 1) {
            return equalsInternal(proxy, params[0]);
        }

        if (method.getName().equals("hashCode") && (params==null || params.length == 0)) {
          return map.hashCode();
        }

        ResultColumn column = method.getAnnotation(ResultColumn.class);
        TypeInformation<?> returnType = ClassTypeInformation.fromReturnTypeOf(method);

        String columnName = column.value();
        if(!map.containsKey( columnName )) {
            throw new NoSuchColumnFoundException( columnName );
        }

        Object columnValue = map.get( columnName );
        if(columnValue==null) return null;

        // If the returned value is a Scala iterable, transform it to a Java iterable first
        Class iterableLikeInterface = implementsInterface("scala.collection.Iterable", columnValue.getClass());
        if (iterableLikeInterface!=null) {
            columnValue = transformScalaIterableToJavaIterable(columnValue, iterableLikeInterface);
        }

        if (returnType.isCollectionLike())
            return new QueryResultBuilder((Iterable)columnValue, converter).to(returnType.getActualType().getType());
        else
            return converter.convert(columnValue, returnType.getType(), mappingPolicy);
    }
    public Object transformScalaIterableToJavaIterable(Object scalaIterable, Class iterableLikeIface) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // This is equivalent to doing this:
        // JavaConversions.asJavaIterable(((IterableLike) columnValue).toIterable());

        Class<?> javaConversions = iterableLikeIface.getClassLoader().loadClass("scala.collection.JavaConversions");
        Method asJavaIterable = javaConversions.getMethod("asJavaIterable", iterableLikeIface);
        Iterable<?> javaIterable = (Iterable<?>) asJavaIterable.invoke(null, scalaIterable);
        return javaIterable;
    }
    private Class implementsInterface(String interfaceName, Class clazz) {
        if(clazz.getCanonicalName().equals(interfaceName)) return clazz;

        Class superclass = clazz.getSuperclass();
        if(superclass != null) {
            Class iface = implementsInterface(interfaceName, superclass);
            if (iface!= null)  return iface;
        }

        for(Class iface : clazz.getInterfaces()) {
            Class superIface = implementsInterface(interfaceName, iface);
            if(superIface!=null)
                return superIface;
        }

        return null;
    }

    private boolean equalsInternal(Object me, Object other) {
        if (other == null) {
            return false;
        }
        if (other.getClass() != me.getClass()) {
            return false;
        }
        InvocationHandler handler = Proxy.getInvocationHandler(other);
        if (!(handler instanceof QueryResultProxy)) return false;
        return ((QueryResultProxy) handler).map.equals(map);
    }
}
