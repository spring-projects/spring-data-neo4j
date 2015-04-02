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

package org.neo4j.ogm.metadata.info;

import org.neo4j.ogm.annotation.*;
import org.neo4j.ogm.metadata.MappingException;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Maintains object to graph mapping details at the class (type) level
 *
 * The ClassInfo object is used to maintain mappings from Java Types->Neo4j Labels
 * thereby allowing the correct labels to be applied to new nodes when they
 * are persisted.
 *
 * The ClassInfo object also maintains a map of FieldInfo and MethodInfo objects
 * that maintain the appropriate information for mapping Java class attributes to Neo4j
 * node properties / paths (node)-[:relationship]->(node), via field or method
 * accessors respectively.
 *
 * Given a type hierarchy, the ClassInfo object guarantees that for any type in that
 * hierarchy, the labels associated with that type will include the labels for
 * all its superclass and interface types as well. This is to avoid the need to iterate
 * through the ClassInfo hierarchy to recover label information.
 *
 * @author Vince Bickers
 */
public class ClassInfo {

    private int majorVersion;
    private int minorVersion;

    private String className;
    private String directSuperclassName;

    private boolean isInterface;
    private boolean isAbstract;
    private boolean isEnum;
    private boolean hydrated;

    private FieldsInfo fieldsInfo = new FieldsInfo();
    private MethodsInfo methodsInfo= new MethodsInfo();
    private AnnotationsInfo annotationsInfo = new AnnotationsInfo();
    private InterfacesInfo interfacesInfo = new InterfacesInfo();

    private ClassInfo directSuperclass;

    private final List<ClassInfo> directSubclasses = new ArrayList<>();
    private final List<ClassInfo> directInterfaces = new ArrayList<>();
    private final List<ClassInfo> directImplementingClasses = new ArrayList<>();


    // todo move this to a factory class
    public ClassInfo(InputStream inputStream) throws IOException {

        DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(inputStream, 1024));

        // Magic
        if (dataInputStream.readInt() != 0xCAFEBABE) {
            return;
        }

        minorVersion = dataInputStream.readUnsignedShort();    //minor version
        majorVersion = dataInputStream.readUnsignedShort();    // major version

        ConstantPool constantPool = new ConstantPool(dataInputStream);

        // Access flags
        int flags = dataInputStream.readUnsignedShort();

        isInterface = (flags & 0x0200) != 0;
        isAbstract = (flags & 0x0400) != 0;
        isEnum = (flags & 0x4000) != 0;

        className = constantPool.lookup(dataInputStream.readUnsignedShort()).replace('/', '.');
        String sce = constantPool.lookup(dataInputStream.readUnsignedShort());
        if (sce != null) {
            directSuperclassName = sce.replace('/', '.');
        }
        interfacesInfo = new InterfacesInfo(dataInputStream, constantPool);
        fieldsInfo = new FieldsInfo(dataInputStream, constantPool);
        methodsInfo = new MethodsInfo(dataInputStream, constantPool);
        annotationsInfo = new AnnotationsInfo(dataInputStream, constantPool);


    }

    /** A class that was previously only seen as a temp superclass of another class can now be fully hydrated. */
    public void hydrate(ClassInfo classInfoDetails) {

        if (!this.hydrated) {
            this.hydrated = true;

            this.isAbstract = classInfoDetails.isAbstract;
            this.isInterface = classInfoDetails.isInterface;
            this.isEnum = classInfoDetails.isEnum;
            this.directSuperclassName = classInfoDetails.directSuperclassName;

            //this.interfaces.addAll(classInfoDetails.interfaces());

            this.interfacesInfo.append(classInfoDetails.interfacesInfo());

            this.annotationsInfo.append(classInfoDetails.annotationsInfo());
            this.fieldsInfo.append(classInfoDetails.fieldsInfo());
            this.methodsInfo.append(classInfoDetails.methodsInfo());
        }
    }

    void extend(ClassInfo classInfo) {
        //this.interfaces.addAll(classInfo.interfaces());
        this.interfacesInfo.append(classInfo.interfacesInfo());

        this.fieldsInfo.append(classInfo.fieldsInfo());
        this.methodsInfo.append(classInfo.methodsInfo());
    }

    /** This class was referenced as a superclass of the given subclass. */
    public ClassInfo(String name, ClassInfo subclass) {
        this.className = name;
        this.hydrated = false;
        addSubclass(subclass);
    }

    /** Connect this class to a subclass. */
    public void addSubclass(ClassInfo subclass) {
        if (subclass.directSuperclass != null && subclass.directSuperclass != this) {
            throw new RuntimeException(subclass.className + " has two superclasses: " + subclass.directSuperclass.className + ", " + this.className);
        }
        subclass.directSuperclass = this;
        this.directSubclasses.add(subclass);
    }

    public boolean hydrated() {
        return hydrated;
    }

    public String name() {
        return className;
    }

    String simpleName() {
        return className.substring(className.lastIndexOf('.') + 1);
    }

    ClassInfo directSuperclass() {
        return directSuperclass;
    }

    /**
     * Retrieves the labels that are applied to nodes in the database that store information about instances of the class. If
     * the class' instances are persisted by a relationship instead of a node then this method returns an empty collection.
     *
     * @return A {@link Collection} of all the labels that apply to the node or an empty list if there aren't any, never
     *         <code>null</code>
     */
    public Collection<String> labels() {
        return collectLabels(new ArrayList<String>());
    }

    public String label() {
        AnnotationInfo annotationInfo = annotationsInfo.get(NodeEntity.CLASS);
        return((annotationInfo != null) ? annotationInfo.get(NodeEntity.LABEL, simpleName()) : simpleName());
    }

    private Collection<String> collectLabels(Collection<String> labelNames) {
        if (!isAbstract || annotationsInfo.get(NodeEntity.CLASS) != null) {
            labelNames.add(label());
        }
        if (directSuperclass != null && !"java.lang.Object".equals(directSuperclass.className)) {
            directSuperclass.collectLabels(labelNames);
        }
        for(ClassInfo interfaceInfo : directInterfaces()) {
            interfaceInfo.collectLabels(labelNames);
        }
        return labelNames;
    }

    public List<ClassInfo> directSubclasses() {
        return directSubclasses;
    }

    public List<ClassInfo> directImplementingClasses() {
        return directImplementingClasses;
    }

    public List<ClassInfo> directInterfaces() {
        return directInterfaces;
    }

    public InterfacesInfo interfacesInfo() {
        return interfacesInfo;
    }

    public Collection<AnnotationInfo> annotations() {
        return annotationsInfo.list();
    }

    public boolean isInterface() {
        return isInterface;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public AnnotationsInfo annotationsInfo() {
        return annotationsInfo;
    }
    public String superclassName() {
        return directSuperclassName;
    }

    public FieldsInfo fieldsInfo() {
        return fieldsInfo;
    }

    public MethodsInfo methodsInfo() {
        return methodsInfo;
    }

    @Override
    public String toString() {
        return name();
    }

    /**
     * The identity field is a field annotated with @NodeId, or if none exists, a field
     * of type Long called 'id'
     *
     * @return A {@link FieldInfo} object representing the identity field never <code>null</code>
     * @throws MappingException if no identity field can be found
     */
    public FieldInfo identityField() {
        for (FieldInfo fieldInfo : fieldsInfo().fields()) {
            AnnotationInfo annotationInfo = fieldInfo.getAnnotations().get(GraphId.CLASS);
            if (annotationInfo != null) {
                if (fieldInfo.getDescriptor().equals("Ljava/lang/Long;")) {
                    return fieldInfo;
                }
            }
        }
        FieldInfo fieldInfo = fieldsInfo().get("id");
        if (fieldInfo != null) {
            if (fieldInfo.getDescriptor().equals("Ljava/lang/Long;")) {
                return fieldInfo;
            }
        }
        throw new MappingException("No identity field found for class: " + this.className);
    }

    /**
     * A property field is any field annotated with @Property, or any field that can be mapped to a
     * node property. The identity field is not a property field.
     *
     * @return A Collection of FieldInfo objects describing the classInfo's property fields
     */
    public Collection<FieldInfo> propertyFields() {
        FieldInfo identityField = identityField();
        Set<FieldInfo> fieldInfos = new HashSet<>();
        for (FieldInfo fieldInfo : fieldsInfo().fields()) {
            if (!fieldInfo.getName().equals(identityField.getName())) {
                AnnotationInfo annotationInfo = fieldInfo.getAnnotations().get(Property.CLASS);
                if (annotationInfo == null) {
                    if (fieldInfo.isSimple()) {
                        fieldInfos.add(fieldInfo);
                    }
                } else {
                    fieldInfos.add(fieldInfo);
                }
            }
        }
        return fieldInfos;
    }

    /**
     * Finds the property field with a specific name from the ClassInfo's property fields
     *
     * @param propertyName the propertyName of the field to find
     * @return A FieldInfo object describing the required property field, or null if it doesn't exist.
     */
    public FieldInfo propertyField(String propertyName) {
        for (FieldInfo fieldInfo : propertyFields()) {
            if (fieldInfo.property().equalsIgnoreCase(propertyName)) {
                return fieldInfo;
            }
        }
        return null;
    }

    /**
     * A relationship field is any field annotated with @Relationship, or any field that cannot be mapped to a
     * node property. The identity field is not a relationship field.
     *
     * @return A Collection of FieldInfo objects describing the classInfo's relationship fields
     */
    public Collection<FieldInfo> relationshipFields() {
        FieldInfo identityField = identityField();
        Set<FieldInfo> fieldInfos = new HashSet<>();
        for (FieldInfo fieldInfo : fieldsInfo().fields()) {
            if (fieldInfo != identityField) {
                AnnotationInfo annotationInfo = fieldInfo.getAnnotations().get(Relationship.CLASS);
                if (annotationInfo == null) {
                    if (!fieldInfo.isSimple()) {
                        fieldInfos.add(fieldInfo);
                    }
                } else {
                    fieldInfos.add(fieldInfo);
                }
            }
        }
        return fieldInfos;
    }

    /**
     * Finds the relationship field with a specific name from the ClassInfo's relationship fields
     *
     * @param relationshipName the relationshipName of the field to find
     * @return A FieldInfo object describing the required relationship field, or null if it doesn't exist.
     */
    public FieldInfo relationshipField(String relationshipName) {
        for (FieldInfo fieldInfo : relationshipFields()) {
            if (fieldInfo.relationship().equalsIgnoreCase(relationshipName)) {
                return fieldInfo;
            }
        }
        return null;
    }

    /**
     * The identity getter is any getter annotated with @NodeId returning a Long, or if none exists, a getter
     * returning Long called 'getId'
     *
     * @return A FieldInfo object representing the identity field or null if it doesn't exist
     */
    public MethodInfo identityGetter() {
        for (MethodInfo methodInfo : methodsInfo().getters()) {
            AnnotationInfo annotationInfo = methodInfo.getAnnotations().get(GraphId.CLASS);
            if (annotationInfo != null) {
                if (methodInfo.getDescriptor().equals("()Ljava/lang/Long;")) {
                    return methodInfo;
                }
            }
        }
        MethodInfo methodInfo = methodsInfo().get("getId");
        if (methodInfo != null) {
            if (methodInfo.getDescriptor().equals("()Ljava/lang/Long;")) {
                return methodInfo;
            }
        }
        return null;
    }

    /**
     * The identity setter is any setter annotated with @NodeId taking a Long parameter, or if none exists, a setter
     * called 'setId' taking a Long parameter
     *
     * @return A FieldInfo object representing the identity field or null if it doesn't exist
     */
    public MethodInfo identitySetter() {
        for (MethodInfo methodInfo : methodsInfo().setters()) {
            AnnotationInfo annotationInfo = methodInfo.getAnnotations().get(GraphId.CLASS);
            if (annotationInfo != null) {
                if (methodInfo.getDescriptor().equals("(Ljava/lang/Long;)V")) {
                    return methodInfo;
                }
            }
        }
        MethodInfo methodInfo = methodsInfo().get("setId");
        if (methodInfo != null) {
            if (methodInfo.getDescriptor().equals("(Ljava/lang/Long;)V")) {
                return methodInfo;
            }
        }
        return null;
    }

    /**
     * A property getter is any getter annotated with @Property, or any getter whose return type can be mapped to a
     * node property. The identity getter is not a property getter.
     *
     * @return A Collection of MethodInfo objects describing the classInfo's property getters
     */
    public Collection<MethodInfo> propertyGetters() {
        MethodInfo identityGetter = identityGetter();
        Set<MethodInfo> propertyGetters = new HashSet<>();
        for (MethodInfo methodInfo : methodsInfo().getters()) {
            if (identityGetter == null || !methodInfo.getName().equals(identityGetter.getName())) {
                AnnotationInfo annotationInfo = methodInfo.getAnnotations().get(Property.CLASS);
                if (annotationInfo == null) {
                    if (methodInfo.isSimpleGetter()) {
                        propertyGetters.add(methodInfo);
                    }
                } else {
                    propertyGetters.add(methodInfo);
                }
            }
        }
        return propertyGetters;
    }

    /**
     * A property setter is any setter annotated with @Property, or any setter whose parameter type can be mapped to a
     * node property. The identity setter is not a property setter.
     *
     * @return A Collection of MethodInfo objects describing the classInfo's property setters
     */
    public Collection<MethodInfo> propertySetters() {
        MethodInfo identitySetter = identitySetter();
        Set<MethodInfo> propertySetters = new HashSet<>();
        for (MethodInfo methodInfo : methodsInfo().setters()) {
            if (identitySetter == null || !methodInfo.getName().equals(identitySetter.getName())) {
                AnnotationInfo annotationInfo = methodInfo.getAnnotations().get(Property.CLASS);
                if (annotationInfo == null) {
                    if (methodInfo.isSimpleSetter()) {
                        propertySetters.add(methodInfo);
                    }
                } else {
                    propertySetters.add(methodInfo);
                }
            }
        }
        return propertySetters;
    }

    /**
     * A relationship getter is any getter annotated with @Relationship, or any getter whose return type cannot be mapped to a
     * node property. The identity getter is not a property getter.
     *
     * @return A Collection of MethodInfo objects describing the classInfo's property getters
     */
    public Collection<MethodInfo> relationshipGetters() {
        MethodInfo identityGetter = identityGetter();
        Set<MethodInfo> relationshipGetters = new HashSet<>();
        for (MethodInfo methodInfo : methodsInfo().getters()) {
            if (identityGetter == null || !methodInfo.getName().equals(identityGetter.getName())) {
                AnnotationInfo annotationInfo = methodInfo.getAnnotations().get(Relationship.CLASS);
                if (annotationInfo == null) {
                    if (!methodInfo.isSimpleGetter()) {
                        relationshipGetters.add(methodInfo);
                    }
                } else {
                    relationshipGetters.add(methodInfo);
                }
            }
        }
        return relationshipGetters;
    }

    /**
     * A relationship setter is any setter annotated with @Relationship, or any setter whose parameter type cannot be mapped to a
     * node property. The identity setter is not a property getter.
     *
     * @return A Collection of MethodInfo objects describing the classInfo's property getters
     */
    public Collection<MethodInfo> relationshipSetters() {
        MethodInfo identitySetter = identitySetter();
        Set<MethodInfo> relationshipSetters = new HashSet<>();
        for (MethodInfo methodInfo : methodsInfo().setters()) {
            if (identitySetter == null || !methodInfo.getName().equals(identitySetter.getName())) {
                AnnotationInfo annotationInfo = methodInfo.getAnnotations().get(Relationship.CLASS);
                if (annotationInfo == null) {
                    if (!methodInfo.isSimpleSetter()) {
                        relationshipSetters.add(methodInfo);
                    }
                } else {
                    relationshipSetters.add(methodInfo);
                }
            }
        }
        return relationshipSetters;
    }

    /**
     * Finds the relationship getter with a specific name from the specified ClassInfo's relationship getters
     *
     * @param relationshipName the relationshipName of the getter to find
     * @return A MethodInfo object describing the required relationship getter, or null if it doesn't exist.
     */
    public MethodInfo relationshipGetter(String relationshipName) {
        for (MethodInfo methodInfo : relationshipGetters()) {
            if (methodInfo.relationship().equalsIgnoreCase(relationshipName)) {
                return methodInfo;
            }
        }
        return null;
    }

    /**
     * Finds the relationship setter with a specific name from the specified ClassInfo's relationship setters
     *
     * @param relationshipName the relationshipName of the setter to find
     * @return A MethodInfo object describing the required relationship setter, or null if it doesn't exist.
     */
    public MethodInfo relationshipSetter(String relationshipName) {
        for (MethodInfo methodInfo : relationshipSetters()) {
            if (methodInfo.relationship().equalsIgnoreCase(relationshipName)) {
                return methodInfo;
            }
        }
        return null;
    }

    /**
     * Finds the property setter with a specific name from the specified ClassInfo's property setters
     *
     * @param propertyName the propertyName of the setter to find
     * @return A MethodInfo object describing the required property setter, or null if it doesn't exist.
     */
    public MethodInfo propertySetter(String propertyName) {
        for (MethodInfo methodInfo : propertySetters()) {
            String match = methodInfo.property();
            if (match.equalsIgnoreCase(propertyName) || match.equalsIgnoreCase("set" + propertyName)) {
                return methodInfo;
            }
        }
        return null;
    }

    /**
     * Finds the property getter with a specific name from the specified ClassInfo's property getters
     *
     * @param propertyName the propertyName of the getter to find
     * @return A MethodInfo object describing the required property getter, or null if it doesn't exist.
     */
    public MethodInfo propertyGetter(String propertyName) {
        for (MethodInfo methodInfo : propertyGetters()) {
            String match = methodInfo.property();
            if (match.equalsIgnoreCase(propertyName) || match.equalsIgnoreCase("get" + propertyName)) {
                return methodInfo;
            }
        }
        return null;
    }

    /**
     *
     * @param fieldInfo
     * @return
     */
    public boolean isScalar(FieldInfo fieldInfo) {
        Field field = getField(fieldInfo);
        return(!Iterable.class.isAssignableFrom(field.getType()) && !fieldInfo.getDescriptor().contains("["));
    }

    /**
     *
     * @param fieldInfo
     * @return
     */
    public Field getField(FieldInfo fieldInfo) {
        try {
            return Class.forName(name()).getDeclaredField(fieldInfo.getName());
        } catch (NoSuchFieldException e) {
            if (directSuperclass() != null) {
                return directSuperclass().getField(fieldInfo);
            } else {
                throw new RuntimeException("Field " + fieldInfo.getName() + " not found in class " + name() + " or any of its superclasses");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     *
     * @param methodInfo
     * @param parameterTypes
     * @return
     */
    public Method getMethod(MethodInfo methodInfo, Class... parameterTypes) {
        try {
            return Class.forName(name()).getMethod(methodInfo.getName(), parameterTypes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Find all setter MethodInfos for the specified ClassInfo whose parameter type matches the supplied class
     *
     * @param parameterType The setter parameter type to look for.
     * @return A {@link List} of {@link MethodInfo} objects that accept the given parameter type, never <code>null</code>
     */
    public List<MethodInfo> findSetters(Class<?> parameterType) {
        String setterSignature = "(L" + parameterType.getName().replace(".", "/") + ";)V";
        List<MethodInfo> methodInfos = new ArrayList<>();
        for (MethodInfo methodInfo : methodsInfo().methods()) {
            if (methodInfo.getDescriptor().equals(setterSignature)) {
                methodInfos.add(methodInfo);
            }
        }
        return methodInfos;
    }

    /**
     * Find all getter MethodInfos for the specified ClassInfo whose return type matches the supplied class
     *
     * @param returnType The getter return type to look for.
     * @return A {@link List} of {@link MethodInfo} objects that return the given type, never <code>null</code>
     */
    public List<MethodInfo> findGetters(Class<?> returnType) {
        String setterSignature = "()L" + returnType.getName().replace(".", "/") + ";";
        List<MethodInfo> methodInfos = new ArrayList<>();
        for (MethodInfo methodInfo : methodsInfo().methods()) {
            if (methodInfo.getDescriptor().equals(setterSignature)) {
                methodInfos.add(methodInfo);
            }
        }
        return methodInfos;
    }

    /**
     * Find all FieldInfos for the specified ClassInfo whose type matches the supplied fieldType
     *
     * @param fieldType The field type to look for
     * @return A {@link List} of {@link FieldInfo} objects that are of the given type, never <code>null</code>
     */
    public List<FieldInfo> findFields(Class<?> fieldType) {
        String fieldSignature = "L" + fieldType.getName().replace(".", "/") + ";";
        List<FieldInfo> fieldInfos = new ArrayList<>();
        for (FieldInfo fieldInfo : fieldsInfo().fields() ) {
            if (fieldInfo.getDescriptor().equals(fieldSignature)) {
                fieldInfos.add(fieldInfo);
            }
        }
        return fieldInfos;
    }

    /**
     * Retrieves a {@link List} of {@link FieldInfo} representing all of the fields that can be iterated over
     * using a "foreach" loop.
     */
    public List<FieldInfo> findIterableFields() {
        List<FieldInfo> fieldInfos = new ArrayList<>();
        try {
            for (FieldInfo fieldInfo : fieldsInfo().fields() ) {
                Class type = getField(fieldInfo).getType();
                if (type.isArray() || Iterable.class.isAssignableFrom(type)) {
                    fieldInfos.add(fieldInfo);
                }
            }
            return fieldInfos;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Finds all fields whose type is equivalent to Array<X> or assignable from Iterable<X>
     * where X is the generic parameter type of the Array or Iterable
     */
    public List<FieldInfo> findIterableFields(Class iteratedType) {
        List<FieldInfo> fieldInfos = new ArrayList<>();
        String typeSignature = "L" + iteratedType.getName().replace('.', '/') + ";";
        String arrayOfTypeSignature = "[" + typeSignature;
        try {
            for (FieldInfo fieldInfo : fieldsInfo().fields() ) {
                if (fieldInfo.getTypeParameterDescriptor() != null) {
                    if (fieldInfo.getTypeParameterDescriptor().equals(typeSignature)) {
                        fieldInfos.add(fieldInfo);
                    }
                } else if (fieldInfo.getDescriptor().equals(arrayOfTypeSignature)) {
                    fieldInfos.add(fieldInfo);
                }
            }
            return fieldInfos;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Finds all setter methods whose parameter signature is equivalent to Array<X> or assignable from Iterable<X>
     * where X is the generic parameter type of the Array or Iterable
     */
    public List<MethodInfo> findIterableSetters(Class iteratedType) {
        List<MethodInfo> methodInfos = new ArrayList<>();
        String typeSignature = "L" + iteratedType.getName().replace('.', '/') + ";";
        String arrayOfTypeSignature = "([" + typeSignature + ")V";
        try {
            for (MethodInfo methodInfo : propertySetters()) {
                if (methodInfo.getTypeParameterDescriptor() != null) {
                    if (methodInfo.getTypeParameterDescriptor().equals(typeSignature)) {
                        methodInfos.add(methodInfo);
                    }
                } else {
                    if (methodInfo.getDescriptor().equals(arrayOfTypeSignature)) {
                        methodInfos.add(methodInfo);
                    }
                }
            }

            for (MethodInfo methodInfo : relationshipSetters()) {
                if (methodInfo.getTypeParameterDescriptor() != null) {
                    if (methodInfo.getTypeParameterDescriptor().equals(typeSignature)) {
                        methodInfos.add(methodInfo);
                    } else {
                        if (methodInfo.getDescriptor().equals(arrayOfTypeSignature)) {
                            methodInfos.add(methodInfo);
                        }
                    }
                }
            }
            return methodInfos;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Finds all getter methods whose parameterised return type is equivalent to Array<X> or assignable from Iterable<X>
     * where X is the generic parameter type of the Array or Iterable
     */
    public List<MethodInfo> findIterableGetters(Class iteratedType) {
        List<MethodInfo> methodInfos = new ArrayList<>();
        String typeSignature = "L" + iteratedType.getName().replace('.', '/') + ";";
        String arrayOfTypeSignature = "()[" + typeSignature;
        try {
            for (MethodInfo methodInfo : propertyGetters()) {
                if (methodInfo.getTypeParameterDescriptor() != null) {
                    if (methodInfo.getTypeParameterDescriptor().equals(typeSignature)) {
                        methodInfos.add(methodInfo);
                    }
                } else {
                    if (methodInfo.getDescriptor().equals(arrayOfTypeSignature)) {
                        methodInfos.add(methodInfo);
                    }
                }
            }

            for (MethodInfo methodInfo : relationshipGetters()) {
                if (methodInfo.getTypeParameterDescriptor() != null) {
                    if (methodInfo.getTypeParameterDescriptor().equals(typeSignature)) {
                        methodInfos.add(methodInfo);
                    } else {
                        if (methodInfo.getDescriptor().equals(arrayOfTypeSignature)) {
                            methodInfos.add(methodInfo);
                        }
                    }
                }
            }
            return methodInfos;
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isTransient() {
        return annotationsInfo.get(Transient.CLASS) != null;
    }
}

