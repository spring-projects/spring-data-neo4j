package org.springframework.data.neo4j.support.conversion;

import org.springframework.data.neo4j.annotation.ResultColumn;
import org.springframework.data.neo4j.conversion.QueryResultBuilder;
import org.springframework.data.neo4j.conversion.ResultConverter;
import org.springframework.data.neo4j.mapping.MappingPolicy;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Given a method or field annotated with the @ResultColumn, this class will
 * extract and return the associated value from the underlying query result.
 * Note: Quite a lot of the code originated from QueryResultProxy and
 * was moved into here.
 *
 * @author Nicki Watt
 * @since 06.08.2013
 */
public class ResultColumnValueExtractor {

    private final Map<String, Object> map;
    private final MappingPolicy mappingPolicy;
    private final ResultConverter converter;

    public ResultColumnValueExtractor(Map<String, Object> map, MappingPolicy mappingPolicy, ResultConverter converter) {
        this.map = map;
        this.mappingPolicy = mappingPolicy;
        this.converter = converter;
    }

    public Object extractFromField(Field field) throws ClassNotFoundException,
        NoSuchMethodException,
        IllegalAccessException,
        InvocationTargetException {
        ResultColumn column = field.getAnnotation(ResultColumn.class);
        TypeInformation<?> returnType = ClassTypeInformation.fromTypeOf(field);
        return extractFromAccessibleObject(column,returnType);
    }

    public Object extractFromMethod(Method method) throws ClassNotFoundException,
            NoSuchMethodException,
            IllegalAccessException,
            InvocationTargetException {
        ResultColumn column = method.getAnnotation(ResultColumn.class);
        TypeInformation<?> returnType = ClassTypeInformation.fromReturnTypeOf(method);
        return extractFromAccessibleObject(column,returnType);
    }

    public Object extractFromAccessibleObject(ResultColumn column, TypeInformation<?> returnType)
            throws ClassNotFoundException,
                    NoSuchMethodException,
                    IllegalAccessException,
                    InvocationTargetException {

        String columnName = column.value();
        if(!map.containsKey( columnName )) {
            throw new NoSuchColumnFoundException( columnName );
        }

        Object columnValue = map.get(columnName);
        if(columnValue==null) return null;

        // If the returned value is a Scala iterable, transform it to a Java iterable first
        Class iterableLikeInterface = implementsInterface("scala.collection.Iterable", columnValue.getClass());
        if (iterableLikeInterface!=null) {
            columnValue = transformScalaIterableToJavaIterable(columnValue, iterableLikeInterface);
        }

        if (returnType.isCollectionLike()) {
             QueryResultBuilder qrb = new QueryResultBuilder((Iterable)columnValue, converter);
             return qrb.to(returnType.getActualType().getType());
        } else
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

}
