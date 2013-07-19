package org.springframework.data.neo4j.support.typesafety;

import org.springframework.data.neo4j.support.ParameterCheck;

/**
 * The type safety policy describes how repositories behave when loading entities from Neo4j.
 *
 * @author spaetzold
 */
public class TypeSafetyPolicy {

    private final TypeSafetyOption option;

    /**
     * Creates a new instance of a type safety policy with the desired type safety option.
     *
     * @param option the type safety option
     */
    public TypeSafetyPolicy(TypeSafetyOption option) {
        ParameterCheck.notNull(option, "option");
        this.option = option;
    }

    /**
     * Creates a new instance of a type safety policy with no type safety enabled.
     */
    public TypeSafetyPolicy() {
        this.option = TypeSafetyOption.NONE;
    }

    /**
     * Indicates if type safety is enabled in the system.
     *
     * @return true if type safety is enabled, else false
     */
    public boolean isTypeSafetyEnabled() {
        return option != TypeSafetyOption.NONE;
    }

    /**
     * Gets the currently defined system type safety option.
     *
     * @return the type safety option
     */
    public TypeSafetyOption getTypeSafetyOption() {
        return option;
    }
}

