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

package org.neo4j.ogm.unit.metadata;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Transient;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.metadata.info.FieldInfo;
import org.neo4j.ogm.metadata.info.MethodInfo;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

/**
 * @author Mark Angrish
 */
public class TransientObjectsTest {

    private MetaData metaData;

    @Before
    public void setUp() {
        metaData = new MetaData("org.neo4j.ogm.unit.metadata");
    }

    @Test
    public void testFieldMarkedWithTransientModifierIsNotInMetaData() {
        ClassInfo classInfo = metaData.classInfo("PersistableClass");
        assertNotNull(classInfo);
        FieldInfo fieldInfo = classInfo.propertyField("transientObject");
        assertNull(fieldInfo);
    }

    @Test
    public void testClassAnnotatedTransientIsExcludedFromMetaData() {
        ClassInfo classInfo = metaData.classInfo("TransientObjectsTest$TransientClass");
        assertNull(classInfo);
    }


    @Test
    public void testMethodAnnotatedTransientIsExcludedFromMetaData() {
        ClassInfo classInfo = metaData.classInfo("PersistableClass");
        MethodInfo methodInfo = classInfo.propertyGetter("transientObject");
        assertNull(methodInfo);
    }

    @Test
    public void testFieldAnnotatedTransientIsExcludedFromMetaData() {
        ClassInfo classInfo = metaData.classInfo("PersistableClass");
        FieldInfo fieldInfo = classInfo.propertyField("chickenCounting");
        assertNull(fieldInfo);
    }

    @NodeEntity(label="PersistableClass")
    public class PersistableClass {

        private Long id;
        private transient String transientObject;

        @Transient
        private Integer chickenCounting;
        
        @Transient
        public String getTransientObject() {
            return transientObject;
        }

        public void setTransientObject(String value) {
            transientObject = value;
        }
    }

    @Transient
    public class TransientClass {
        private Long id;
    }
}
