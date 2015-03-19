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

package org.neo4j.ogm.unit.mapper;

import org.junit.Test;
import org.neo4j.ogm.domain.education.School;
import org.neo4j.ogm.domain.education.Teacher;
import org.neo4j.ogm.mapper.EntityMemo;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.info.ClassInfo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vince Bickers
 */
public class ObjectMemoTest {

    private static final MetaData metaData = new MetaData("org.neo4j.ogm.domain.education");
    private static final EntityMemo objectMemo = new EntityMemo();

    @Test
    public void testUnchangedObjectDetected() {

        ClassInfo classInfo = metaData.classInfo(Teacher.class.getName());
        Teacher mrsJones = new Teacher();

        objectMemo.remember(mrsJones, classInfo);

        mrsJones.setId(115L); // the id field must not be part of the memoised property list

        assertTrue(objectMemo.remembered(mrsJones, classInfo));

    }

    @Test
    public void testChangedPropertyDetected() {

        ClassInfo classInfo = metaData.classInfo(Teacher.class.getName());
        Teacher teacher = new Teacher("Miss White");

        objectMemo.remember(teacher, classInfo);

        teacher.setId(115L); // the id field must not be part of the memoised property list
        teacher.setName("Mrs Jones"); // the teacher's name property has changed.

        assertFalse(objectMemo.remembered(teacher, classInfo));
    }

    @Test
    public void testRelatedObjectChangeDoesNotAffectNodeMemoisation() {

        ClassInfo classInfo = metaData.classInfo(Teacher.class.getName());
        Teacher teacher = new Teacher("Miss White");

        objectMemo.remember(teacher, classInfo);

        teacher.setId(115L); // the id field must not be part of the memoised property list
        teacher.setSchool(new School("Roedean")); // a related object does not affect the property list.

        assertTrue(objectMemo.remembered(teacher, classInfo));
    }



}
