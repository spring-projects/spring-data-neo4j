package org.springframework.data.neo4j.mapping;

/**
 * Sets the system to return null if a entity should be loaded which is not of the requested type.
 */
public class NullReturningTypeSafetyPolicy extends DefaultTypeSafetyPolicy {

    public NullReturningTypeSafetyPolicy() {
        this.option = TypeSafetyOption.RETURNS_NULL;
    }
}
