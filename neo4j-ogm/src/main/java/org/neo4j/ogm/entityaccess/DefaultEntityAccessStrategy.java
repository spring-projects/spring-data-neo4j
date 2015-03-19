/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.entityaccess;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.FieldInfo;
import org.neo4j.ogm.metadata.info.MethodInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Default implementation of {@link EntityAccessStrategy} that looks up information from {@link ClassInfo} in the following order.
 * <ol>
 * <li>Annotated Method (getter/setter)</li>
 * <li>Annotated Field</li>
 * <li>Plain Method (getter/setter)</li>
 * <li>Plain Field</li>
 * </ol>
 * The rationale is simply that we want annotations, whether on fields or on methods, to always take precedence, and we want to
 * use methods in preference to field access, because in many cases hydrating an object means more than just assigning values to
 * fields.
 */
public class DefaultEntityAccessStrategy implements EntityAccessStrategy {

    private final Logger logger = LoggerFactory.getLogger(DefaultEntityAccessStrategy.class);

    /** Used internally to hide differences in object construction from strategy algorithm. */
    private static interface AccessorFactory<T> {
        T makeMethodAccessor(MethodInfo methodInfo);
        T makeFieldAccessor(FieldInfo fieldInfo);
    }

    @Override
    public EntityAccess getPropertyWriter(final ClassInfo classInfo, String propertyName) {
        MethodInfo setterInfo = classInfo.propertySetter(propertyName);
        return determinePropertyAccessor(classInfo, propertyName, setterInfo, new AccessorFactory<EntityAccess>() {
            @Override
            public EntityAccess makeMethodAccessor(MethodInfo methodInfo) {
                return new MethodWriter(classInfo, methodInfo);
            }

            @Override
            public EntityAccess makeFieldAccessor(FieldInfo fieldInfo) {
                return new FieldWriter(classInfo, fieldInfo);
            }
        });
    }

    @Override
    public PropertyReader getPropertyReader(final ClassInfo classInfo, String propertyName) {
        MethodInfo getterInfo = classInfo.propertyGetter(propertyName);
        return determinePropertyAccessor(classInfo, propertyName, getterInfo, new AccessorFactory<PropertyReader>() {
            @Override
            public PropertyReader makeMethodAccessor(MethodInfo methodInfo) {
                return new MethodReader(classInfo, methodInfo);
            }
            @Override
            public PropertyReader makeFieldAccessor(FieldInfo fieldInfo) {
                return new FieldReader(classInfo, fieldInfo);
            }
        });
    }

    private <T> T determinePropertyAccessor(ClassInfo classInfo, String propertyName, MethodInfo accessorMethodInfo,
            AccessorFactory<T> factory) {
        if (accessorMethodInfo != null) {
            if (accessorMethodInfo.getAnnotations().isEmpty()) {
                // if there's an annotated field then we should prefer that over the non-annotated method
                FieldInfo fieldInfo = classInfo.propertyField(propertyName);
                if (fieldInfo != null && !fieldInfo.getAnnotations().isEmpty()) {
                    return factory.makeFieldAccessor(fieldInfo);
                }
            }
            return factory.makeMethodAccessor(accessorMethodInfo);
        }

        // fall back to the field if method cannot be found
        FieldInfo fieldInfo = classInfo.propertyField(propertyName);
        if (fieldInfo != null) {
            return factory.makeFieldAccessor(fieldInfo);
        }
        return null;
    }

    @Override
    public EntityAccess getRelationalWriter(ClassInfo classInfo, String relationshipType, Object parameter) {

        // 1st, try to find a scalar method annotated with the relationship type.
        MethodInfo methodInfo = classInfo.relationshipSetter(relationshipType);
        if (methodInfo != null && !methodInfo.getAnnotations().isEmpty()) {

            if (methodInfo.isTypeOf(parameter.getClass()) ||
                methodInfo.isParameterisedTypeOf(parameter.getClass()) ||
                methodInfo.isArrayOf(parameter.getClass())) {
                    return new MethodWriter(classInfo, methodInfo);

            }
        }

        // 2nd, try to find a scalar or vector field annotated as the neo4j relationship type
        FieldInfo fieldInfo = classInfo.relationshipField(relationshipType);
        if (fieldInfo != null && !fieldInfo.getAnnotations().isEmpty()) {
            if (fieldInfo.isTypeOf(parameter.getClass()) ||
                fieldInfo.isParameterisedTypeOf(parameter.getClass()) ||
                fieldInfo.isArrayOf(parameter.getClass())) {
                    return new FieldWriter(classInfo, fieldInfo);
            }
        }

        // 3rd, try to find a "setXYZ" method where XYZ is derived from the relationship type
        methodInfo = classInfo.relationshipSetter(relationshipType);
        if (methodInfo != null) {
            if (methodInfo.isTypeOf(parameter.getClass()) ||
                    methodInfo.isParameterisedTypeOf(parameter.getClass()) ||
                    methodInfo.isArrayOf(parameter.getClass())) {
                return new MethodWriter(classInfo, methodInfo);

            }
        }

        // 4th, try to find a "XYZ" field name where XYZ is derived from the relationship type
        fieldInfo = classInfo.relationshipField(relationshipType);
        if (fieldInfo != null) {
            if (fieldInfo.isTypeOf(parameter.getClass()) ||
                    fieldInfo.isParameterisedTypeOf(parameter.getClass()) ||
                    fieldInfo.isArrayOf(parameter.getClass())) {
                return new FieldWriter(classInfo, fieldInfo);
            }
        }

        // 5th, try to find a unique setter method that takes the parameter
        List<MethodInfo> methodInfos = classInfo.findSetters(parameter.getClass());
        if (methodInfos.size() == 1) {
            return new MethodWriter(classInfo, methodInfos.iterator().next());
        }

        // 6th, try to find a unique field that has the same type as the parameter
        List<FieldInfo> fieldInfos = classInfo.findFields(parameter.getClass());
        if (fieldInfos.size() == 1) {
            return new FieldWriter(classInfo, fieldInfos.iterator().next());
        }

        return null;
    }

    @Override
    public RelationalReader getRelationalReader(ClassInfo classInfo, String relationshipType) {
        // 1st, try to find a method annotated with the relationship type.
        MethodInfo methodInfo = classInfo.relationshipGetter(relationshipType);
        if (methodInfo != null && !methodInfo.getAnnotations().isEmpty()) {
            return new MethodReader(classInfo, methodInfo);
        }

        // 2nd, try to find a field called or annotated as the neo4j relationship type
        FieldInfo fieldInfo = classInfo.relationshipField(relationshipType);
        if (fieldInfo != null && !fieldInfo.getAnnotations().isEmpty()) {
            return new FieldReader(classInfo, fieldInfo);
        }

        // 3rd, try to find a "getXYZ" method where XYZ is derived from the given relationship type
        if (methodInfo != null) {
            return new MethodReader(classInfo, methodInfo);
        }

        // 4th, try to find a "XYZ" field name where XYZ is derived from the relationship type
        if (fieldInfo != null) {
            return new FieldReader(classInfo, fieldInfo);
        }

        //
        return null;
    }

    @Override
    public Collection<PropertyReader> getPropertyReaders(ClassInfo classInfo) {
        // do we care about "implicit" fields?  i.e., setX/getX with no matching X field

        Collection<PropertyReader> readers = new ArrayList<>();
        for (FieldInfo fieldInfo : classInfo.propertyFields()) {
            MethodInfo getterInfo = classInfo.propertyGetter(fieldInfo.property());
            if (getterInfo != null) {
                if (!getterInfo.getAnnotations().isEmpty() || fieldInfo.getAnnotations().isEmpty()) {
                    readers.add(new MethodReader(classInfo, getterInfo));
                    continue;
                }
            }
            readers.add(new FieldReader(classInfo, fieldInfo));
        }
        return readers;
    }

    @Override
    public Collection<RelationalReader> getRelationalReaders(ClassInfo classInfo) {
        Collection<RelationalReader> readers = new ArrayList<>();
        for (FieldInfo fieldInfo : classInfo.relationshipFields()) {
            MethodInfo getterInfo = classInfo.methodsInfo().get(inferGetterName(fieldInfo));

            if (getterInfo != null) {
                if (!getterInfo.getAnnotations().isEmpty() || fieldInfo.getAnnotations().isEmpty()) {
                    readers.add(new MethodReader(classInfo, getterInfo));
                    continue;
                }
            }
            readers.add(new FieldReader(classInfo, fieldInfo));
        }
        return readers;
    }

    private static String inferGetterName(FieldInfo fieldInfo) {
        StringBuilder getterNameBuilder = new StringBuilder(fieldInfo.getName());
        getterNameBuilder.setCharAt(0, Character.toUpperCase(fieldInfo.getName().charAt(0)));
        return getterNameBuilder.insert(0, "get").toString();
    }

    @Override
    public EntityAccess getIterableWriter(ClassInfo classInfo, Class<?> parameterType) {
        MethodInfo methodInfo = getIterableSetterMethodInfo(classInfo, parameterType);
        if (methodInfo != null) {
            return new MethodWriter(classInfo, methodInfo);
        }
        FieldInfo fieldInfo = getIterableFieldInfo(classInfo, parameterType);
        if (fieldInfo != null) {
            return new FieldWriter(classInfo, fieldInfo);
        }
        return null;
    }

    @Override
    public RelationalReader getIterableReader(ClassInfo classInfo, Class<?> parameterType) {
        MethodInfo methodInfo = getIterableGetterMethodInfo(classInfo, parameterType);
        if (methodInfo != null) {
            return new MethodReader(classInfo, methodInfo);
        }
        FieldInfo fieldInfo = getIterableFieldInfo(classInfo, parameterType);
        if (fieldInfo != null) {
            return new FieldReader(classInfo, fieldInfo);
        }
        return null;
    }

    @Override
    public PropertyReader getIdentityPropertyReader(ClassInfo classInfo) {
        return new FieldReader(classInfo, classInfo.identityField());
    }

    @Override
    public RelationalReader getEndNodeReader(ClassInfo relationshipEntityClassInfo) {
        for (FieldInfo fieldInfo : relationshipEntityClassInfo.relationshipFields()) {
            if (fieldInfo.getAnnotations().get(EndNode.CLASS) != null) {
                return new FieldReader(relationshipEntityClassInfo, fieldInfo);
            }
        }
        logger.warn("Failed to find an @EndNode on " + relationshipEntityClassInfo);
        return null;
    }

    @Override
    public RelationalReader getStartNodeReader(ClassInfo relationshipEntityClassInfo) {
        for (FieldInfo fieldInfo : relationshipEntityClassInfo.relationshipFields()) {
            if (fieldInfo.getAnnotations().get(StartNode.CLASS) != null) {
                return new FieldReader(relationshipEntityClassInfo, fieldInfo);
            }
        }
        logger.warn("Failed to find an @StartNode on " + relationshipEntityClassInfo);
        return null;
    }

    private MethodInfo getIterableSetterMethodInfo(ClassInfo classInfo, Class<?> parameterType) {
        List<MethodInfo> methodInfos = classInfo.findIterableSetters(parameterType);
        if (methodInfos.size() == 1) {
            return methodInfos.iterator().next();
        }

        if (methodInfos.size() > 0) {
            logger.warn("Cannot map iterable of {} to instance of {}. More than one potential matching setter found.",
                    parameterType, classInfo.name());
        }

        return null;
    }

    private MethodInfo getIterableGetterMethodInfo(ClassInfo classInfo, Class<?> parameterType) {
        List<MethodInfo> methodInfos = classInfo.findIterableGetters(parameterType);
        if (methodInfos.size() == 1) {
            return methodInfos.iterator().next();
        }

        if (methodInfos.size() > 0) {
            logger.warn("Cannot map iterable of {} to instance of {}.  More than one potential matching getter found.",
                    parameterType, classInfo.name());
        }
        return null;
    }

    private FieldInfo getIterableFieldInfo(ClassInfo classInfo, Class<?> parameterType) {
        List<FieldInfo> fieldInfos = classInfo.findIterableFields(parameterType);
        if (fieldInfos.size() == 1) {
            return fieldInfos.iterator().next();
        }

        if (fieldInfos.size() > 0) {
            logger.warn("Cannot map iterable of {} to instance of {}. More than one potential matching field found.",
                    parameterType, classInfo.name());
        }

        return null;
    }

}
