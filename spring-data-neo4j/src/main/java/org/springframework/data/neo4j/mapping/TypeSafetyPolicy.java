package org.springframework.data.neo4j.mapping;

/**
 * The type safety policy describes how repositories behave when loading entities from Neo4j.
 *
 * @author spaetzold
 */
public interface TypeSafetyPolicy {

    enum TypeSafetyOption {

        /** Sets the system to not be type safe (default setting). */
        NONE,

        /** Sets the system to return null if a entity should be loaded which is not of the requested type. */
        RETURNS_NULL,

        /** Sets the system to throw an exception if a entity should be loaded which is not of the requested type. */
        THROWS_EXCEPTION
    }

    /**
     * Indicates if type safety is enabled in the system.
     *
     * @return true if type safety is enabled, else false
     */
    boolean isTypeSafetyEnabled();

    /**
     * Gets the currently defined system type safety option.
     *
     * @return the type safety option
     */
    TypeSafetyOption getTypeSafetyOption();
}
