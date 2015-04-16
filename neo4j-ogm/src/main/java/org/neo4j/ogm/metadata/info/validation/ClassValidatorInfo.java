package org.neo4j.ogm.metadata.info.validation;

import org.neo4j.ogm.metadata.info.*;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * The ClassValidatorInfo transform information from ClassInfo
 * for Model Validator.
 *
 */

public class ClassValidatorInfo {
    private String name;
    private Collection<Member> annotations;
    private Collection<Member> fields;
    private Collection<Member> methods;
    private Collection<Member> getters;
    private Collection<Member> setters;

    public ClassValidatorInfo(ClassInfo classInfo) {
        this.name = classInfo.name();
        this.annotations = getAnnotations(classInfo);
        this.fields = getFields(classInfo);
        this.methods = getMethods(classInfo);
        this.getters = getGetters(classInfo);
        this.setters = getSetters(classInfo);
    }

    public String getName() {
        return name;
    }

    public Collection<Member> getAnnotations() {
        return annotations;
    }

    public Collection<Member> getFields() {
        return fields;
    }

    public Collection<Member> getMethods() {
        return methods;
    }

    public Collection<Member> getGetters() {
        return getters;
    }

    public Collection<Member> getSetters() {
        return setters;
    }

    private Collection<Member> getAnnotations(ClassInfo classInfo) {
        Collection<Member> annotations = new ArrayList<>();

        for(AnnotationInfo annotationInfo : classInfo.annotations()) {
            annotations.add(new Member(annotationInfo.getName(), classInfo.name()));
        }

        return annotations;
    }

    private Collection<Member> getMethods(ClassInfo classInfo) {
        MethodsInfo methodsInfo = classInfo.methodsInfo();

        return getMembers(methodsInfo.methods());
    }

    private Collection<Member> getFields(ClassInfo classInfo) {
        Collection<Member> methods = new ArrayList<>();

        FieldsInfo fieldsInfo = classInfo.fieldsInfo();

        for(FieldInfo fieldInfo : fieldsInfo.fields()) {
            String name = fieldInfo.getName();
            String type = fieldInfo.getDescriptor();

            Collection<Member> annotations = new ArrayList<>();

            ObjectAnnotations annotations1 = fieldInfo.getAnnotations();

            for(AnnotationInfo annotationInfo : annotations1.getAll()) {
                annotations.add(new Member(annotationInfo.getName(), name));
            }

            methods.add(new Member(name, type, annotations));
        }

        return methods;
    }

    private Collection<Member> getGetters(ClassInfo classInfo) {
        MethodsInfo methodsInfo = classInfo.methodsInfo();

        return getMembers(methodsInfo.getters());
    }

    private Collection<Member> getSetters(ClassInfo classInfo) {
        MethodsInfo methodsInfo = classInfo.methodsInfo();

        return getMembers(methodsInfo.setters());
    }

    private Collection<Member> getMembers(Collection<MethodInfo> methodInfos) {
        Collection<Member> methods = new ArrayList<>();

        for(MethodInfo methodInfo : methodInfos) {
            String name = methodInfo.getName();
            String type = methodInfo.getDescriptor();

            Collection<Member> methodAnnotations = new ArrayList<>();

            ObjectAnnotations annotations = methodInfo.getAnnotations();

            for(AnnotationInfo annotationInfo : annotations.getAll()) {
                methodAnnotations.add(new Member(annotationInfo.getName(), name));
            }

            methods.add(new Member(name, type, methodAnnotations));
        }

        return methods;
    }
}
