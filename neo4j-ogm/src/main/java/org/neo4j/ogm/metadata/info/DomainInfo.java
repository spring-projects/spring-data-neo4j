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

import org.neo4j.ogm.metadata.ClassPathScanner;
import org.neo4j.ogm.metadata.MappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class DomainInfo implements ClassFileProcessor {

    private static final String dateSignature = "java/util/Date";
    private static final String bigDecimalSignature = "java/math/BigDecimal";
    private static final String bigIntegerSignature = "java/math/BigInteger";
    private static final String byteArraySignature = "[B";
    private static final String byteArrayWrapperSignature = "[Ljava/lang/Byte";
    private static final String arraySignature = "[L";
    private static final String collectionSignature = "L";

    private final List<String> classPaths = new ArrayList<>();

    private final Map<String, ClassInfo> classNameToClassInfo = new HashMap<>();
    private final Map<String, ArrayList<ClassInfo>> annotationNameToClassInfo = new HashMap<>();
    private final Map<String, ArrayList<ClassInfo>> interfaceNameToClassInfo = new HashMap<>();

    private final Set<String> enumTypes = new HashSet<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassFileProcessor.class);

    public DomainInfo(String... packages) {
        long now = -System.currentTimeMillis();
        load(packages);

        LOGGER.info(classNameToClassInfo.entrySet().size() + " classes loaded in " + (now + System.currentTimeMillis()) + " milliseconds");
    }

    private void buildAnnotationNameToClassInfoMap() {

        LOGGER.info("Building annotation class map");
        for (ClassInfo classInfo : classNameToClassInfo.values()) {
            for (AnnotationInfo annotation : classInfo.annotations()) {
                ArrayList<ClassInfo> classInfoList = annotationNameToClassInfo.get(annotation.getName());
                if (classInfoList == null) {
                    annotationNameToClassInfo.put(annotation.getName(), classInfoList = new ArrayList<>());
                }
                classInfoList.add(classInfo);
            }
        }
    }

    private void buildInterfaceNameToClassInfoMap() {
        LOGGER.info("Building interface class map for " + classNameToClassInfo.values().size() + " classes");
        for (ClassInfo classInfo : classNameToClassInfo.values()) {
            LOGGER.debug(" - " + classInfo.simpleName() + " implements " + classInfo.interfacesInfo().list().size() + " interfaces");
            for (InterfaceInfo iface : classInfo.interfacesInfo().list()) {
                ArrayList<ClassInfo> classInfoList = interfaceNameToClassInfo.get(iface.name());
                if (classInfoList == null) {
                    interfaceNameToClassInfo.put(iface.name(), classInfoList = new ArrayList<>());
                }
                LOGGER.debug("   - " + iface.name());
                classInfoList.add(classInfo);
            }
        }
    }

    private void registerDefaultTypeConverters() {

        LOGGER.info("Registering default type converters...");
        for (ClassInfo classInfo : classNameToClassInfo.values()) {
            if (!classInfo.isEnum() && !classInfo.isInterface()) {
                registerDefaultFieldConverters(classInfo);
                registerDefaultMethodConverters(classInfo);
            }
        }
    }



    public void finish() {

        LOGGER.info("Starting Post-processing phase");

        buildAnnotationNameToClassInfoMap();
        buildInterfaceNameToClassInfoMap();

        registerDefaultTypeConverters();

        List<ClassInfo> transientClasses = new ArrayList<>();

        for (ClassInfo classInfo : classNameToClassInfo.values()) {

            if (classInfo.name() == null || classInfo.name().equals("java.lang.Object")) continue;

            LOGGER.debug("Post-processing: " + classInfo.name());

            if (classInfo.isTransient()) {
                LOGGER.info(" - Registering @Transient baseclass: " + classInfo.name());
                transientClasses.add(classInfo);
                continue;
            }

            if (classInfo.superclassName() == null || classInfo.superclassName().equals("java.lang.Object")) {
                extend(classInfo, classInfo.directSubclasses());
            }

            for(InterfaceInfo interfaceInfo : classInfo.interfacesInfo().list()) {
                implement(classInfo, interfaceInfo);
            }
        }

        LOGGER.debug("Checking for @Transient classes....");

        // find transient interfaces
        Collection<ArrayList<ClassInfo>> interfaceInfos = interfaceNameToClassInfo.values();
        for (ArrayList<ClassInfo> classInfos : interfaceInfos) {
            for (ClassInfo classInfo : classInfos) {
                if(classInfo.isTransient()) {
                    LOGGER.info("Registering @Transient baseclass: " + classInfo.name());
                    transientClasses.add(classInfo);
                }
            }
        }

        // remove all transient class hierarchies
        for (ClassInfo transientClass : transientClasses) {
            removeTransientClass(transientClass);
        }

        LOGGER.info("Post-processing complete");

    }

    private void removeTransientClass(ClassInfo transientClass) {
        if (transientClass != null && !transientClass.name().equals("java.lang.Object")) {
            LOGGER.info("Removing @Transient class: " + transientClass.name());
            classNameToClassInfo.remove(transientClass.name());
            for (ClassInfo transientChild : transientClass.directSubclasses()) {
                removeTransientClass(transientChild);
            }
            for (ClassInfo transientChild : transientClass.directImplementingClasses()) {
                removeTransientClass(transientChild);
            }
        }

    }


    private void extend(ClassInfo superclass, List<ClassInfo> subclasses) {
        for (ClassInfo subclass : subclasses) {
            subclass.extend(superclass);
            extend(subclass, subclass.directSubclasses());
        }
    }

    private void implement(ClassInfo implementingClass, InterfaceInfo interfaceInfo) {

        ClassInfo interfaceClass = classNameToClassInfo.get(interfaceInfo.name());

        if (interfaceClass != null) {
            if (!implementingClass.directInterfaces().contains(interfaceClass)) {
                LOGGER.debug(" - Setting " + implementingClass.simpleName() + " implements " + interfaceClass.simpleName());
                implementingClass.directInterfaces().add(interfaceClass);
            }

            if (!interfaceClass.directImplementingClasses().contains(implementingClass)) {
                interfaceClass.directImplementingClasses().add(implementingClass);
            }

            for (ClassInfo subClassInfo : implementingClass.directSubclasses()) {
                implement(subClassInfo, interfaceInfo);
            }

        } else {
            LOGGER.warn(" - No ClassInfo found for interface class: " + interfaceInfo.name());
        }

    }

    public void process(final InputStream inputStream) throws IOException {

        ClassInfo classInfo = new ClassInfo(inputStream);

        String className = classInfo.name();
        String superclassName = classInfo.superclassName();

        LOGGER.debug("Processing: " + className + " -> " + superclassName);

        if (className != null) {

            ClassInfo thisClassInfo = classNameToClassInfo.get(className);

            if (thisClassInfo == null) {
                thisClassInfo = classInfo;
                classNameToClassInfo.put(className, thisClassInfo);
            }

            if (!thisClassInfo.hydrated()) {

                thisClassInfo.hydrate(classInfo);

                ClassInfo superclassInfo = classNameToClassInfo.get(superclassName);
                if (superclassInfo == null) {
                    classNameToClassInfo.put(superclassName, new ClassInfo(superclassName, thisClassInfo));
                } else {
                    superclassInfo.addSubclass(thisClassInfo);
                }
            }

            if (thisClassInfo.isEnum()) {
                String enumSignature = thisClassInfo.name().replace(".", "/");
                LOGGER.info("Registering enum class: " + enumSignature);
                enumTypes.add(enumSignature);
            }
        }
    }

    private void load(String... packages) {

        classPaths.clear();
        classNameToClassInfo.clear();
        annotationNameToClassInfo.clear();
        interfaceNameToClassInfo.clear();

        for (String packageName : packages) {
            String path = packageName.replaceAll("\\.", File.separator);
            classPaths.add(path);
        }

        new ClassPathScanner().scan(classPaths, this);

    }

    public ClassInfo getClass(String fqn) {
        return classNameToClassInfo.get(fqn);
    }

    // all classes, including interfaces will be registered in classNameToClassInfo map
    public ClassInfo getClassSimpleName(String fullOrPartialClassName) {
        ClassInfo classInfo = getClassInfo(fullOrPartialClassName, classNameToClassInfo);
        return classInfo;
    }


    public ClassInfo getClassInfoForInterface(String fullOrPartialClassName) {
        ClassInfo classInfo = getClassSimpleName(fullOrPartialClassName);
        if (classInfo.isInterface()) {
            return classInfo;
        }
        return null;
    }

    private ClassInfo getClassInfo(String fullOrPartialClassName, Map<String, ClassInfo> infos) {
        ClassInfo match = null;
        for (String fqn : infos.keySet()) {
            if (fqn.endsWith("." + fullOrPartialClassName) || fqn.equals(fullOrPartialClassName)) {
                if (match == null) {
                    match = infos.get(fqn);
                } else {
                    throw new MappingException("More than one class has simple name: " + fullOrPartialClassName);
                }
            }
        }
        return match;
    }

    public List<ClassInfo> getClassInfosWithAnnotation(String annotation) {
        return annotationNameToClassInfo.get(annotation);
    }

    private void registerDefaultMethodConverters(ClassInfo classInfo) {
        for (MethodInfo methodInfo : classInfo.methodsInfo().methods()) {
            if (!methodInfo.hasConverter()) {
                if (methodInfo.getDescriptor().contains(dateSignature)
                        || (methodInfo.getTypeParameterDescriptor()!=null && methodInfo.getTypeParameterDescriptor().contains(dateSignature))) {
                    setDateMethodConverter(methodInfo);
                }
                else if (methodInfo.getDescriptor().contains(bigIntegerSignature)
                        || (methodInfo.getTypeParameterDescriptor()!=null && methodInfo.getTypeParameterDescriptor().contains(bigIntegerSignature))) {
                    setBigIntegerMethodConverter(methodInfo);
                }
                else if (methodInfo.getDescriptor().contains(bigDecimalSignature)
                        || (methodInfo.getTypeParameterDescriptor()!=null && methodInfo.getTypeParameterDescriptor().contains(bigDecimalSignature))) {

                    setBigDecimalMethodConverter(methodInfo);
                }
                else if (methodInfo.getDescriptor().contains(byteArraySignature)) {
                    methodInfo.setConverter(ConvertibleTypes.getByteArrayBase64Converter());
                }
                else if (methodInfo.getDescriptor().contains(byteArrayWrapperSignature)) {
                    methodInfo.setConverter(ConvertibleTypes.getByteArrayWrapperBase64Converter());
                }
                else {
                    for (String enumSignature : enumTypes) {
                        if (methodInfo.getDescriptor().contains(enumSignature) || (methodInfo.getTypeParameterDescriptor()!=null && methodInfo.getTypeParameterDescriptor().contains(enumSignature))) {
                            setEnumMethodConverter(methodInfo, enumSignature);
                        }
                    }
                }
            }
        }
    }

    private void setEnumMethodConverter(MethodInfo methodInfo, String enumSignature) {
        if(methodInfo.getDescriptor().contains(arraySignature)) {
            methodInfo.setConverter(ConvertibleTypes.getEnumArrayConverter(enumSignature));
        }
        else if(methodInfo.getDescriptor().contains(collectionSignature) && methodInfo.isCollection()) {
            methodInfo.setConverter(ConvertibleTypes.getEnumCollectionConverter(enumSignature,methodInfo.getCollectionClassname()));
        }
        else {
            methodInfo.setConverter(ConvertibleTypes.getEnumConverter(enumSignature));
        }
    }

    private void setBigDecimalMethodConverter(MethodInfo methodInfo) {
        if(methodInfo.getDescriptor().contains(arraySignature)) {
            methodInfo.setConverter(ConvertibleTypes.getBigDecimalArrayConverter());
        }
        else if(methodInfo.getDescriptor().contains(collectionSignature) && methodInfo.isCollection()) {
            methodInfo.setConverter(ConvertibleTypes.getBigDecimalCollectionConverter(methodInfo.getCollectionClassname()));
        }
        else {
            methodInfo.setConverter(ConvertibleTypes.getBigDecimalConverter());
        }
    }

    private void setBigIntegerMethodConverter(MethodInfo methodInfo) {
        if(methodInfo.getDescriptor().contains(arraySignature)) {
            methodInfo.setConverter(ConvertibleTypes.getBigIntegerArrayConverter());
        }
        else if(methodInfo.getDescriptor().contains(collectionSignature) && methodInfo.isCollection()) {
            methodInfo.setConverter(ConvertibleTypes.getBigIntegerCollectionConverter(methodInfo.getCollectionClassname()));
        }
        else {
            methodInfo.setConverter(ConvertibleTypes.getBigIntegerConverter());
        }
    }

    private void setDateMethodConverter(MethodInfo methodInfo) {
        if(methodInfo.getDescriptor().contains(arraySignature)) {
            methodInfo.setConverter(ConvertibleTypes.getDateArrayConverter());
        }
        else if(methodInfo.getDescriptor().contains(collectionSignature) && methodInfo.isCollection()) {
            methodInfo.setConverter(ConvertibleTypes.getDateCollectionConverter(methodInfo.getCollectionClassname()));
        }
        else {
            methodInfo.setConverter(ConvertibleTypes.getDateConverter());
        }
    }

    private void registerDefaultFieldConverters(ClassInfo classInfo) {
        for (FieldInfo fieldInfo : classInfo.fieldsInfo().fields()) {
            if (!fieldInfo.hasConverter()) {
                if (fieldInfo.getDescriptor().contains(dateSignature)
                        || (fieldInfo.getTypeParameterDescriptor()!=null && fieldInfo.getTypeParameterDescriptor().contains(dateSignature))) {
                    setDateFieldConverter(fieldInfo);
                }
                else if (fieldInfo.getDescriptor().contains(bigIntegerSignature)
                        || (fieldInfo.getTypeParameterDescriptor()!=null && fieldInfo.getTypeParameterDescriptor().contains(bigIntegerSignature))) {
                    setBigIntegerFieldConverter(fieldInfo);
                }
                else if (fieldInfo.getDescriptor().contains(bigDecimalSignature)
                        || (fieldInfo.getTypeParameterDescriptor()!=null && fieldInfo.getTypeParameterDescriptor().contains(bigDecimalSignature))) {
                    setBigDecimalConverter(fieldInfo);
                }
                else if (fieldInfo.getDescriptor().contains(byteArraySignature)) {
                    fieldInfo.setConverter(ConvertibleTypes.getByteArrayBase64Converter());
                }
                else if (fieldInfo.getDescriptor().contains(byteArrayWrapperSignature)) {
                    fieldInfo.setConverter(ConvertibleTypes.getByteArrayWrapperBase64Converter());
                }
                else {
                    for (String enumSignature : enumTypes) {
                        if (fieldInfo.getDescriptor().contains(enumSignature) || (fieldInfo.getTypeParameterDescriptor()!=null && fieldInfo.getTypeParameterDescriptor().contains(enumSignature))) {
                            setEnumFieldConverter(fieldInfo, enumSignature);
                        }
                    }
                }
            }
        }
    }

    private void setEnumFieldConverter(FieldInfo fieldInfo, String enumSignature) {
        if(fieldInfo.getDescriptor().contains(arraySignature)) {
            fieldInfo.setConverter(ConvertibleTypes.getEnumArrayConverter(enumSignature));
        }
        else if(fieldInfo.getDescriptor().contains(collectionSignature) && fieldInfo.isCollection()) {
            fieldInfo.setConverter(ConvertibleTypes.getEnumCollectionConverter(enumSignature,fieldInfo.getCollectionClassname()));
        }
        else {
            fieldInfo.setConverter(ConvertibleTypes.getEnumConverter(enumSignature));
        }
    }

    private void setBigDecimalConverter(FieldInfo fieldInfo) {
        if(fieldInfo.getDescriptor().contains(arraySignature)) {
            fieldInfo.setConverter(ConvertibleTypes.getBigDecimalArrayConverter());
        }
        else if(fieldInfo.getDescriptor().contains(collectionSignature) && fieldInfo.isCollection()) {
            fieldInfo.setConverter(ConvertibleTypes.getBigDecimalCollectionConverter(fieldInfo.getCollectionClassname()));
        }
        else {
            fieldInfo.setConverter(ConvertibleTypes.getBigDecimalConverter());
        }
    }

    private void setBigIntegerFieldConverter(FieldInfo fieldInfo) {
        if(fieldInfo.getDescriptor().contains(arraySignature)) {
            fieldInfo.setConverter(ConvertibleTypes.getBigIntegerArrayConverter());
        }
        else if(fieldInfo.getDescriptor().contains(collectionSignature) && fieldInfo.isCollection()) {
            fieldInfo.setConverter(ConvertibleTypes.getBigIntegerCollectionConverter(fieldInfo.getCollectionClassname()));
        }
        else {
            fieldInfo.setConverter(ConvertibleTypes.getBigIntegerConverter());
        }
    }

    private void setDateFieldConverter(FieldInfo fieldInfo) {
        if(fieldInfo.getDescriptor().contains(arraySignature)) {
            fieldInfo.setConverter(ConvertibleTypes.getDateArrayConverter());
        }
        else if(fieldInfo.getDescriptor().contains(collectionSignature) && fieldInfo.isCollection()) {
            fieldInfo.setConverter(ConvertibleTypes.getDateCollectionConverter(fieldInfo.getCollectionClassname()));
        }
        else {
            fieldInfo.setConverter(ConvertibleTypes.getDateConverter());
        }
    }

}
