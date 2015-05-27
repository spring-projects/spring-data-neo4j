package org.neo4j.ogm.unit.metadata.validation;

import org.junit.Test;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.DomainInfo;
import org.neo4j.ogm.metadata.info.validation.ClassValidatorInfo;

import static org.junit.Assert.assertEquals;

public class ClassValidatorInfoTest {

    @Test
    public void shouldCreateClassValidatorInfoFromClassInfo() {
        DomainInfo domainInfo = new DomainInfo("org.neo4j.ogm.domain.validations.Relationship.correct");

        ClassInfo classInfo = domainInfo.getClass(org.neo4j.ogm.domain.validations.Relationship.correct.Relationship.class.getName());

        ClassValidatorInfo classValidatorInfo = new ClassValidatorInfo(classInfo);

        assertEquals("org.neo4j.ogm.domain.validations.Relationship.correct.Relationship", classValidatorInfo.getName());
        assertEquals(1, classValidatorInfo.getAnnotations().size());
        assertEquals(3, classValidatorInfo.getFields().size());
        assertEquals(4, classValidatorInfo.getMethods().size());
        assertEquals(3, classValidatorInfo.getGetters().size());
        assertEquals(1, classValidatorInfo.getSetters().size());
    }
}
