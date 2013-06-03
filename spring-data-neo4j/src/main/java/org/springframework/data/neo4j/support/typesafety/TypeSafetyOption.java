package org.springframework.data.neo4j.support.typesafety;

/**
 * Enum of all type safety options which are available in the system.
 *
 * @author spaetzold
 */
public enum TypeSafetyOption {

    /** Sets the system to not be type safe (default setting). */
    NONE,

    /** Sets the system to return null if a entity should be loaded which is not of the requested type. */
    RETURNS_NULL,

    /** Sets the system to throw an exception if a entity should be loaded which is not of the requested type. */
    THROWS_EXCEPTION

}
