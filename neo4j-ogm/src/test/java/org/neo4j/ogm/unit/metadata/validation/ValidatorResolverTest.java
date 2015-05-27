package org.neo4j.ogm.unit.metadata.validation;

import org.junit.Test;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.Transient;
import org.neo4j.ogm.metadata.info.validation.NodeEntityValidator;
import org.neo4j.ogm.metadata.info.validation.RelationshipEntityValidator;
import org.neo4j.ogm.metadata.info.validation.EntityValidator;
import org.neo4j.ogm.metadata.info.validation.ValidatorResolver;

import static org.junit.Assert.assertEquals;

public class ValidatorResolverTest {

    @Test
    public void shouldReturnValidatorForRelationshipEntity() {
        ValidatorResolver resolver = new ValidatorResolver();

        EntityValidator entityValidator = resolver.getValidator(RelationshipEntity.CLASS);

        assertEquals(RelationshipEntityValidator.class, entityValidator.getClass());
    }

    @Test
    public void shouldReturnValidatorForNodeEntity() {
        ValidatorResolver resolver = new ValidatorResolver();

        EntityValidator entityValidator = resolver.getValidator(NodeEntity.CLASS);

        assertEquals(NodeEntityValidator.class, entityValidator.getClass());
    }

    @Test
    public void shouldReturnNullForNotExistingValidatorForTransient() {
        ValidatorResolver resolver = new ValidatorResolver();

        EntityValidator entityValidator = resolver.getValidator(Transient.CLASS);

        assertEquals(null, entityValidator);
    }
}