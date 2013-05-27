package org.springframework.data.neo4j.mapping;

/**
 * Default type safety policy which sets the system to be not type safe.
 */
public class DefaultTypeSafetyPolicy implements TypeSafetyPolicy {

    protected TypeSafetyOption option;

    public DefaultTypeSafetyPolicy() {
        this.option = TypeSafetyOption.NONE;
    }

    @Override
    public boolean isTypeSafetyEnabled() {
        return option != TypeSafetyOption.NONE;
    }

    @Override
    public TypeSafetyOption getTypeSafetyOption() {
        return option;
    }
}
