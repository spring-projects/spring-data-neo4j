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
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.FieldInfo;
import org.neo4j.ogm.metadata.info.MethodInfo;
import org.neo4j.ogm.typeconversion.AttributeConverter;
import org.neo4j.ogm.typeconversion.DateLongConverter;
import org.neo4j.ogm.typeconversion.DateStringConverter;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestDateConversion {

    private static final MetaData metaData = new MetaData("org.neo4j.ogm.domain.convertible.date");
    private static final ClassInfo memoInfo = metaData.classInfo("Memo");

    @Test
    public void assertFieldDateConversionToISO8601FormatByDefault() {
        FieldInfo fieldInfo = memoInfo.propertyField("recorded");
        assertTrue(fieldInfo.hasConverter());
        AttributeConverter attributeConverter = fieldInfo.converter();
        assertTrue(attributeConverter.getClass().isAssignableFrom(DateStringConverter.class));
        assertEquals("1970-01-01T00:00:00.000Z", attributeConverter.toGraphProperty(new Date(0)));
    }

    @Test
    public void assertFieldDateConversionWithUserDefinedFormat() {
        FieldInfo fieldInfo = memoInfo.propertyField("actioned");
        assertTrue(fieldInfo.hasConverter());
        AttributeConverter attributeConverter = fieldInfo.converter();
        assertTrue(attributeConverter.getClass().isAssignableFrom(DateStringConverter.class));
        assertEquals("1970-01-01", attributeConverter.toGraphProperty(new Date(0)));
    }

    @Test
    public void assertFieldDateLongConversion() {
        FieldInfo fieldInfo = memoInfo.propertyField("closed");
        assertTrue(fieldInfo.hasConverter());
        AttributeConverter attributeConverter = fieldInfo.converter();
        assertTrue(attributeConverter.getClass().isAssignableFrom(DateLongConverter.class));
        Date date = new Date(0);
        Long value = (Long) attributeConverter.toGraphProperty(date);
        assertEquals(new Long(0), value);
    }

    @Test
    public void assertFieldCustomTypeConversion() {
        FieldInfo fieldInfo = memoInfo.propertyField("approved");
        assertTrue(fieldInfo.hasConverter());
        AttributeConverter attributeConverter = fieldInfo.converter();
        assertEquals("20090213113130", attributeConverter.toGraphProperty(new Date(1234567890123L)));
    }

    @Test
    public void assertMethodDateConversionToISO8601FormatByDefault() {
        MethodInfo methodInfo = memoInfo.propertyGetter("recorded");
        assertTrue(methodInfo.hasConverter());
        AttributeConverter attributeConverter = methodInfo.converter();
        assertTrue(attributeConverter.getClass().isAssignableFrom(DateStringConverter.class));
        assertEquals("1970-01-01T00:00:00.000Z", attributeConverter.toGraphProperty(new Date(0)));
    }

    @Test
    public void assertMethodDateConversionWithUserDefinedFormat() {
        MethodInfo methodInfo = memoInfo.propertySetter("actioned");
        assertTrue(methodInfo.hasConverter());
        AttributeConverter attributeConverter = methodInfo.converter();
        assertTrue(attributeConverter.getClass().isAssignableFrom(DateStringConverter.class));
        Date date = new Date(0);
        String value = (String) attributeConverter.toGraphProperty(date);
        assertEquals("1970-01-01", value);
    }

    @Test
    public void assertMethodDateLongConversion() {
        MethodInfo methodInfo = memoInfo.propertyGetter("closed");
        assertTrue(methodInfo.hasConverter());
        AttributeConverter attributeConverter = methodInfo.converter();
        assertTrue(attributeConverter.getClass().isAssignableFrom(DateLongConverter.class));
        Date date = new Date(0);
        assertEquals(new Long(0), attributeConverter.toGraphProperty(date));
    }

    @Test
    public void assertMethodCustomTypeConversion() {
        MethodInfo methodInfo = memoInfo.propertySetter("approved");
        assertTrue(methodInfo.hasConverter());
        AttributeConverter attributeConverter = methodInfo.converter();
        Date date = new Date(1234567890123L);
        assertEquals("20090213113130", attributeConverter.toGraphProperty(date));
    }

    @Test
    public void assertConvertingNullGraphPropertyWorksCorrectly() {
        MethodInfo methodInfo = memoInfo.propertySetter("approved");
        assertTrue(methodInfo.hasConverter());
        AttributeConverter attributeConverter = methodInfo.converter();
        assertEquals(null, attributeConverter.toEntityAttribute(null));
    }

    @Test
    public void assertConvertingNullAttributeWorksCorrectly() {
        MethodInfo methodInfo = memoInfo.propertySetter("approved");
        assertTrue(methodInfo.hasConverter());
        AttributeConverter attributeConverter = methodInfo.converter();
        assertEquals(null, attributeConverter.toGraphProperty(null));
    }

}
