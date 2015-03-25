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
 */
public class DomainInfo implements ClassFileProcessor {

    private static final String dateSignature = "java/util/Date";
    private static final String bigDecimalSignature = "java/math/BigDecimal";
    private static final String bigIntegerSignature = "java/math/BigInteger";
    private static final String byteArraySignature = "[B";
    private static final String byteArrayWrapperSignature = "[Ljava/lang/Byte";

    private final List<String> classPaths = new ArrayList<>();
    private final Map<String, ClassInfo> classNameToClassInfo = new HashMap<>();
    private final Map<String, ArrayList<ClassInfo>> annotationNameToClassInfo = new HashMap<>();
    private final Map<String, ClassInfo> interfaceNameToClassInfo = new HashMap<>();

    private final Set<String> enumTypes = new HashSet<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(ClassFileProcessor.class);

    public DomainInfo(String... packages) {
        long now = -System.currentTimeMillis();
        load(packages);
        LOGGER.info(classNameToClassInfo.entrySet().size() + " classes loaded in " + (now + System.currentTimeMillis()) + " milliseconds");
    }

    private void buildAnnotationNameToClassInfoMap() {
        // A <-[:has_annotation]- T
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

    private void registerDefaultTypeConverters() {

        for (ClassInfo classInfo : classNameToClassInfo.values()) {
            if (!classInfo.isEnum() && !classInfo.isInterface()) {

                for (FieldInfo fieldInfo : classInfo.fieldsInfo().fields()) {
                    if (!fieldInfo.hasConverter()) {
                        if (fieldInfo.getDescriptor().contains(dateSignature)) {
                            fieldInfo.setConverter(ConvertibleTypes.getDateConverter());
                        }
                        else if (fieldInfo.getDescriptor().contains(bigIntegerSignature)) {
                            fieldInfo.setConverter(ConvertibleTypes.getBigIntegerConverter());
                        }
                        else if (fieldInfo.getDescriptor().contains(bigDecimalSignature)) {
                            fieldInfo.setConverter(ConvertibleTypes.getBigDecimalConverter());
                        }
                        else if (fieldInfo.getDescriptor().contains(byteArraySignature)) {
                            fieldInfo.setConverter(ConvertibleTypes.getByteArrayBase64Converter());
                        }
                        else if (fieldInfo.getDescriptor().contains(byteArrayWrapperSignature)) {
                            fieldInfo.setConverter(ConvertibleTypes.getByteArrayWrapperBase64Converter());
                        }
                        else {
                            for (String enumSignature : enumTypes) {
                                if (fieldInfo.getDescriptor().contains(enumSignature)) {
                                    fieldInfo.setConverter(ConvertibleTypes.getEnumConverter(enumSignature));
                                }
                            }
                        }
                    }
                }

                for (MethodInfo methodInfo : classInfo.methodsInfo().methods()) {
                    if (!methodInfo.hasConverter()) {
                        if (methodInfo.getDescriptor().contains(dateSignature)) {
                            methodInfo.setConverter(ConvertibleTypes.getDateConverter());
                        }
                        else if (methodInfo.getDescriptor().contains(bigIntegerSignature)) {
                            methodInfo.setConverter(ConvertibleTypes.getBigIntegerConverter());
                        }
                        else if (methodInfo.getDescriptor().contains(bigDecimalSignature)) {
                            methodInfo.setConverter(ConvertibleTypes.getBigDecimalConverter());
                        }
                        else if (methodInfo.getDescriptor().contains(byteArraySignature)) {
                            methodInfo.setConverter(ConvertibleTypes.getByteArrayBase64Converter());
                        }
                        else if (methodInfo.getDescriptor().contains(byteArrayWrapperSignature)) {
                            methodInfo.setConverter(ConvertibleTypes.getByteArrayWrapperBase64Converter());
                        }
                        else {
                            for (String enumSignature : enumTypes) {
                                if (methodInfo.getDescriptor().contains(enumSignature)) {
                                    methodInfo.setConverter(ConvertibleTypes.getEnumConverter(enumSignature));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void finish() {
        buildAnnotationNameToClassInfoMap();
        registerDefaultTypeConverters();
        List<ClassInfo> transientClasses = new ArrayList<>();
        for (ClassInfo classInfo : classNameToClassInfo.values()) {
            if (classInfo.name() == null || classInfo.name().equals("java.lang.Object")) continue;

            if (classInfo.isTransient()) {
                LOGGER.info("Registering @Transient baseclass: " + classInfo.name());
                transientClasses.add(classInfo);
                continue;
            }

            if (classInfo.superclassName() == null || classInfo.superclassName().equals("java.lang.Object")) {
                extend(classInfo, classInfo.directSubclasses());
            }

            if(classInfo.interfacesInfo()!=null) {
                for(InterfaceInfo interfaceInfo : classInfo.interfacesInfo().list()) {
                    implement(classInfo, interfaceInfo);
                }
            }
        }
        // find transient interfaces
        for(ClassInfo classInfo : interfaceNameToClassInfo.values()) {
            if(classInfo.isTransient()) {
                LOGGER.info("Registering @Transient baseclass: " + classInfo.name());
                transientClasses.add(classInfo);
            }
        }
        // remove all transient class hierarchies
        for (ClassInfo transientClass : transientClasses) {
            removeTransientClass(transientClass);
        }

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
        ClassInfo interfaceClassInfo = interfaceNameToClassInfo.get(interfaceInfo.name());
        if(interfaceClassInfo!=null) {
            implementingClass.directInterfaces().add(interfaceNameToClassInfo.get(interfaceInfo.name()));
            interfaceNameToClassInfo.get(interfaceInfo.name()).directImplementingClasses().add(implementingClass);
            for (InterfaceInfo superInterface : interfaceClassInfo.interfacesInfo().list()) {
                implement(implementingClass, superInterface);
            }
        }
    }

    public void process(final InputStream inputStream) throws IOException {

        ClassInfo classInfo = new ClassInfo(inputStream);

        String className = classInfo.name();
        String superclassName = classInfo.superclassName();

        LOGGER.debug("processing: " + className + " -> " + superclassName);

        if (className != null) {
            if (classInfo.isInterface()) {
                if(interfaceNameToClassInfo.get(className)==null) {
                    interfaceNameToClassInfo.put(className,classInfo);
                }
            } else {
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

    public ClassInfo getClassSimpleName(String fullOrPartialClassName) {
       return getInterfaceOrClassInfo(fullOrPartialClassName,classNameToClassInfo);
    }

    public List<ClassInfo> getClassInfosWithAnnotation(String annotation) {
        return annotationNameToClassInfo.get(annotation);
    }

    public ClassInfo getClassInfoForInterface(String fullOrPartialClassName) {
        return getInterfaceOrClassInfo(fullOrPartialClassName,interfaceNameToClassInfo);
    }

    private ClassInfo getInterfaceOrClassInfo(String fullOrPartialClassName, Map<String,ClassInfo> infos) {
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

}
