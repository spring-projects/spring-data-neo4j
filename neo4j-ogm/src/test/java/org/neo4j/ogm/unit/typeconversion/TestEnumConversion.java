/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.unit.typeconversion;

import org.junit.Test;
import org.neo4j.ogm.domain.convertible.enums.*;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.FieldInfo;
import org.neo4j.ogm.metadata.info.MethodInfo;
import org.neo4j.ogm.typeconversion.AttributeConverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestEnumConversion {

    private static final MetaData metaData = new MetaData("org.neo4j.ogm.domain.convertible.enums");
    private static final ClassInfo algebraInfo = metaData.classInfo("Algebra");
    private static final ClassInfo personInfo = metaData.classInfo("Person");

    @Test
    public void testSaveFieldWithAnnotatedConverter() {
        FieldInfo fieldInfo = algebraInfo.propertyField("numberSystem");
        assertTrue(fieldInfo.hasConverter());

        Algebra algebra = new Algebra();
        algebra.setNumberSystem(NumberSystem.NATURAL);
        assertEquals("N", algebra.getNumberSystem().getDomain());
        String value = (String) fieldInfo.converter().toGraphProperty(algebra.getNumberSystem());
        // the converted enum value that will be stored as a neo4j node / rel property
        assertEquals("NATURAL", value);
    }

    @Test
    public void testLoadFieldWithAnnotatedConverter() {
        FieldInfo fieldInfo = algebraInfo.propertyField("numberSystem");
        assertTrue(fieldInfo.hasConverter());
        // a node / rel property value loaded from neo4j, to be stored in on an enum
        String value = "INTEGER";
        Algebra algebra = new Algebra();
        algebra.setNumberSystem((NumberSystem) fieldInfo.converter().toEntityAttribute(value));

        assertEquals(NumberSystem.INTEGER, algebra.getNumberSystem());
        assertEquals("Z", algebra.getNumberSystem().getDomain());
    }

    @Test
    public void testCustomConverter() {
        MethodInfo methodInfo = algebraInfo.propertyGetter("numberSystem");
        assertTrue(methodInfo.hasConverter());
        assertEquals(NumberSystemDomainConverter.class, methodInfo.converter().getClass());

        String domain = "Z";  // an algebraic domain (i.e. the integers)

        Algebra algebra = new Algebra();
        algebra.setNumberSystem((NumberSystem) methodInfo.converter().toEntityAttribute(domain));

        assertEquals(NumberSystem.INTEGER, algebra.getNumberSystem());
        assertEquals("Z", algebra.getNumberSystem().getDomain());
    }

    @Test
    public void testGenderFieldWithAutoDetectedConverter() {

        Person bob = new Person();
        bob.setGender(Gender.MALE);

        FieldInfo fieldInfo = personInfo.propertyField("gender");

        assertTrue(fieldInfo.hasConverter());
        assertEquals("MALE", fieldInfo.converter().toGraphProperty(bob.getGender()));

    }

    @Test
    public void testGenderSetterWithAutoDetectedConverter() {
        Person bob = new Person();
        MethodInfo methodInfo = personInfo.propertySetter("gender");
        assertTrue(methodInfo.hasConverter());
        bob.setGender((Gender) methodInfo.converter().toEntityAttribute("MALE"));
        assertEquals(Gender.MALE, bob.getGender());

    }

    @Test
    public void testGenderGetterWithAutoDetectedConverter() {
        Person bob = new Person();
        bob.setGender(Gender.MALE);
        MethodInfo methodInfo = personInfo.propertyGetter("gender");
        assertTrue(methodInfo.hasConverter());
        assertEquals("MALE", methodInfo.converter().toGraphProperty(bob.getGender()));
    }

    @Test
    public void assertConvertingNullGraphPropertyWorksCorrectly() {
        MethodInfo methodInfo = personInfo.propertyGetter("gender");
        assertTrue(methodInfo.hasConverter());
        AttributeConverter attributeConverter = methodInfo.converter();
        assertEquals(null, attributeConverter.toEntityAttribute(null));
    }

    @Test
    public void assertConvertingNullAttributeWorksCorrectly() {
        MethodInfo methodInfo = personInfo.propertyGetter("gender");
        assertTrue(methodInfo.hasConverter());
        AttributeConverter attributeConverter = methodInfo.converter();
        assertEquals(null, attributeConverter.toGraphProperty(null));
    }

}
