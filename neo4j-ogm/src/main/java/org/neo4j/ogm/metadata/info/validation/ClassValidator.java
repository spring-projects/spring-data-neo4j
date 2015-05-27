package org.neo4j.ogm.metadata.info.validation;

import java.util.Collection;

/**
 *
 * The ClassValidator is entry point for model validation.
 *
 * Providing information about class members and annotations
 * for further validations. Those information are stored in
 * Member class within Members class.
 *
 */

public class ClassValidator {

    private String name;
    private Collection<Member> annotations;
    private Collection<Member> fields;
    private Collection<Member> methods;
    private Collection<Member> getters;
    private Collection<Member> setters;

    public ClassValidator(ClassValidatorInfo classValidatorInfo) {
        this.name = classValidatorInfo.getName();
        this.annotations = classValidatorInfo.getAnnotations();
        this.fields = classValidatorInfo.getFields();
        this.methods = classValidatorInfo.getMethods();
        this.getters = classValidatorInfo.getGetters();
        this.setters = classValidatorInfo.getSetters();
    }

    public String getName() {
        return name;
    }

    public Members Annotations(String ... names) {
        return new Members(annotations, Member.translateNamesToMembers(names));
    }

    public Members Methods(String ... names) {
        return new Members(methods, Member.translateNamesToMembers(names));
    }

    public Members Fields(String ... names) {
        return new Members(fields, Member.translateNamesToMembers(names));
    }

    public Members Getters(String ... names) {
        return new Members(getters, Member.translateNamesToMembers(names));
    }

    public Members Setters(String ... names) {
        return new Members(setters, Member.translateNamesToMembers(names));
    }
}
