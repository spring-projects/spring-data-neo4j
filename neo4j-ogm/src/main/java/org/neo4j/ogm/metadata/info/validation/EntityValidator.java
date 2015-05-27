package org.neo4j.ogm.metadata.info.validation;

import org.neo4j.ogm.metadata.info.ClassInfo;

/**
 *
 * Abstract Validator class for Class Type's validators.
 *
 */

public abstract class EntityValidator {
    protected StringBuilder errorMessage;
    protected ClassValidator clazz;
    protected boolean areValid;

    public EntityValidator() {
        this.errorMessage = new StringBuilder();
    }

    public abstract void validate(ClassInfo classInfo);

    public String getErrorMessage() {
        return errorMessage.toString();
    }
}
