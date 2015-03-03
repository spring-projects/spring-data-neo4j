/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
