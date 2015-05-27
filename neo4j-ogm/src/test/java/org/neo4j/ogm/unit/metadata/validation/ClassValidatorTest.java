package org.neo4j.ogm.unit.metadata.validation;

import org.junit.Test;
import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.annotation.Transient;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.DomainInfo;
import org.neo4j.ogm.metadata.info.validation.ClassValidator;
import org.neo4j.ogm.metadata.info.validation.ClassValidatorInfo;

import static junit.framework.TestCase.assertTrue;

public class ClassValidatorTest {

    @Test
    public void shouldBeValidForComplexRelationshipEntity() {
        DomainInfo domainInfo = new DomainInfo("org.neo4j.ogm.domain.validations.Relationship.correct");

        ClassInfo classInfo = domainInfo.getClass(org.neo4j.ogm.domain.validations.Relationship.correct.Relationship.class.getName());

        ClassValidator classValidator = new ClassValidator(new ClassValidatorInfo(classInfo));

        boolean isValid = classValidator.Annotations(RelationshipEntity.CLASS).Required()
                          && classValidator.Fields().Annotations(StartNode.CLASS, EndNode.CLASS).Required();

        assertTrue(isValid);
    }

    @Test
    public void shouldBeValidForRelationshipEntityWithoutTransientAnnotation() {
        DomainInfo domainInfo = new DomainInfo("org.neo4j.ogm.domain.validations.Relationship.correct");

        ClassInfo classInfo = domainInfo.getClass(org.neo4j.ogm.domain.validations.Relationship.correct.Relationship.class.getName());

        ClassValidator classValidator = new ClassValidator(new ClassValidatorInfo(classInfo));

        boolean isInvalid = classValidator.Fields(Transient.CLASS).Forbidden();

        assertTrue(isInvalid);
    }

    @Test
    public void shouldBeValidForRelationshipEntityWithOnlyOneEndNode() {
        DomainInfo domainInfo = new DomainInfo("org.neo4j.ogm.domain.validations.Relationship.correct");

        ClassInfo classInfo = domainInfo.getClass(org.neo4j.ogm.domain.validations.Relationship.correct.Relationship.class.getName());

        ClassValidator classValidator = new ClassValidator(new ClassValidatorInfo(classInfo));

        boolean isValid = classValidator.Fields().Annotations(EndNode.CLASS).Unique();

        assertTrue(isValid);
    }

    @Test
    public void shouldBeValidForRelationshipEntityWithStartNodeAndEndNode() {
        DomainInfo domainInfo = new DomainInfo("org.neo4j.ogm.domain.validations.Relationship.correct");

        ClassInfo classInfo = domainInfo.getClass(org.neo4j.ogm.domain.validations.Relationship.correct.Relationship.class.getName());

        ClassValidator classValidator = new ClassValidator(new ClassValidatorInfo(classInfo));

        boolean isValid = classValidator.Fields().Annotations(StartNode.CLASS, EndNode.CLASS).Required();

        assertTrue(isValid);
    }

    @Test
    public void shouldBeValidForRelationshipEntityWithLongTypeIdField() {
        DomainInfo domainInfo = new DomainInfo("org.neo4j.ogm.domain.validations.Relationship.correct");

        ClassInfo classInfo = domainInfo.getClass(org.neo4j.ogm.domain.validations.Relationship.correct.Relationship.class.getName());

        ClassValidator classValidator = new ClassValidator(new ClassValidatorInfo(classInfo));

        boolean isLong = classValidator.Fields("id").Type(Long.class);

        assertTrue(isLong);
    }

    @Test
    public void shouldBeValidForRelationshipEntityWithOnlyOneStartNodeAndEndNode() {
        DomainInfo domainInfo = new DomainInfo("org.neo4j.ogm.domain.validations.Relationship.correct");

        ClassInfo classInfo = domainInfo.getClass(org.neo4j.ogm.domain.validations.Relationship.correct.Relationship.class.getName());

        ClassValidator classValidator = new ClassValidator(new ClassValidatorInfo(classInfo));

        boolean isValid = classValidator.Fields().Annotations(StartNode.CLASS, EndNode.CLASS).Unique();

        assertTrue(isValid);
    }
}

