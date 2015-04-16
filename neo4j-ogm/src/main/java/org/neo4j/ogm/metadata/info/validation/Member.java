package org.neo4j.ogm.metadata.info.validation;

import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * The Member is data holder for model validation.
 *
 * Providing all necessary information for validations.
 * Also Type validation of Member, which is not applicable for
 * Annotations.
 *
 */

public class Member {
    private String name;
    private String type;
    private String belongs;
    private Collection<Member> annotations;

    public Member(String name) {
        this.name = name;
    }

    public Member(String name, String belongs) {
        this.name = name;
        this.belongs = belongs;
    }

    public Member(String name, String type, Collection<Member> annotation) {
        this.name = name;
        this.type = type;
        this.annotations = annotation;
    }

    public Collection<Member> getAnnotations() {
        return annotations;
    }

    public boolean isType(Class<?> clazz) {
        String typeSignature = "L" + clazz.getName().replace(".", "/") + ";";
        if (this.type != null && this.type.equals(typeSignature)) {
            return true;
        }

        return false;
    }

    public static Collection<Member> translateNamesToMembers(String... names) {
        Collection<Member> members = new ArrayList<>();

        for (String name : names) {
            members.add(new Member(name));
        }

        return members;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Member)) return false;

        Member that = (Member) o;

        if (!name.equals(that.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    public String getBelongs() {
        return belongs;
    }
}
