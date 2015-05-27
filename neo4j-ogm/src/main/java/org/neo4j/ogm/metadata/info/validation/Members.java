package org.neo4j.ogm.metadata.info.validation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/**
 *
 * The Members contains DSL methods for validation over Members.
 *
 * Members contains all members of class and required members,
 * which user wants to validate.
 *
 */

public class Members {
    Collection<Member> current;
    Collection<Member> required;

    public Members(Collection<Member> current, Collection<Member> required) {
        this.current = current;
        this.required = required;
    }

    /** Returns Members for Member Annotations */
    public Members Annotations(String ... names) {
        return new Members(getAnnotations(), Member.translateNamesToMembers(names));
    }

    /** Validate if Members contains member of specific type */
    public boolean Type(Class<?> clazz) {
        for (Member member : current) {
            if(member.isType(clazz)) {
                return true;
            }
        }

        return false;
    }

    /** Validate if all required members are present */
    public boolean Required() {
        return current.containsAll(required);
    }

    /** Validate if required members are not present */
    public boolean Forbidden() {
        return !Required();
    }

    /** Validate if required members are present only once */
    public boolean Unique() {
        Collection<Member> requiredFromCurrent = new ArrayList<>();

        for (Member require : this.required) {
            int frequency = Collections.frequency(current, require);

            if (frequency != 1) {
                return false;
            }
        }

        for (Member member : this.current) {
            if(this.required.contains(member)) {
                requiredFromCurrent.add(member);
            }
        }

        HashSet uniqueName = new HashSet<>();

        for (Member member : requiredFromCurrent) {
            if(uniqueName.contains(member.getBelongs())) {
                return false;
            } else {
                uniqueName.add(member.getBelongs());
            }
        }

        return true;
    }

    private Collection<Member> getAnnotations() {
        Collection<Member> annotations = new ArrayList<>();

        for(Member member : current) {
            annotations.addAll(member.getAnnotations());
        }

        return annotations;
    }
}