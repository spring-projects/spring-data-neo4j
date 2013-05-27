package org.springframework.data.neo4j.mapping;

/**
 * Sets the system to throw an exception if a entity should be loaded which is not of the requested type.
 */
public class ExceptionThrowingTypeSafetyPolicy extends DefaultTypeSafetyPolicy {

    public ExceptionThrowingTypeSafetyPolicy() {
        this.option = TypeSafetyOption.THROWS_EXCEPTION;
    }
}
