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
        TypeInformation<?> classInfo = ClassTypeInformation.from(field.getDeclaringClass());
        TypeInformation<?> fieldInfo = classInfo.getProperty(field.getName());
        return extractFromAccessibleObject(fieldInfo, columnNameFor(field));
    }

    private String columnNameFor(Field field) {
        ResultColumn column = field.getAnnotation(ResultColumn.class);
        if (column != null) return column.value();
        return field.getName();
    }

    public Object extractFromMethod(Method method) throws ClassNotFoundException,
            NoSuchMethodException,
            IllegalAccessException,
            InvocationTargetException {
        TypeInformation<?> returnType = ClassTypeInformation.fromReturnTypeOf(method);
        return extractFromAccessibleObject(returnType, columnNameFor(method));
    }

    private String columnNameFor(Method method) {
        ResultColumn column = method.getAnnotation(ResultColumn.class);
        if (column != null) return column.value();
        String name = method.getName();
        if (name.startsWith("get")) name = name.substring(3);
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    public Object extractFromAccessibleObject(TypeInformation<?> returnType, String columnName)
            throws ClassNotFoundException,
                    NoSuchMethodException,
                    IllegalAccessException,
                    InvocationTargetException {

        if(!map.containsKey(columnName)) {
            throw new NoSuchColumnFoundException(columnName);
        }

        Object columnValue = map.get(columnName);
        if(columnValue==null) return null;

        if (returnType.isCollectionLike()) {
             QueryResultBuilder qrb = new QueryResultBuilder((Iterable)columnValue, converter);
             return qrb.to(returnType.getActualType().getType()).as(returnType.getType());
        } else
           return converter.convert(columnValue, returnType.getType(), mappingPolicy);
    }
}
