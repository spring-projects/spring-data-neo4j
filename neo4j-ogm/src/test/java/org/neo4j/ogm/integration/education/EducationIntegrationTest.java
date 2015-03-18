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
package org.neo4j.ogm.integration.education;


import org.junit.BeforeClass;
import org.junit.Test;
import org.neo4j.ogm.domain.education.Course;
import org.neo4j.ogm.domain.education.Student;
import org.neo4j.ogm.integration.InMemoryServerTest;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.SessionFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class EducationIntegrationTest extends InMemoryServerTest {

    @BeforeClass
    public static void init() throws IOException {
        setUp();
        session = new SessionFactory("org.neo4j.ogm.domain.education").openSession("http://localhost:" + neoPort);
    }

    @Test
    public void loadingCourseByPropertyShouldNotLoadOtherEntitiesWithSamePropertyValue() {
        //create a course
        Course course = new Course("CompSci");
        //create a student with the same name as the course
        Student student = new Student("CompSci");
        //relate them so they're both in the mappingContext
        course.setStudents(Collections.singletonList(student));
        session.save(course);

        //fetch Courses by name
        Collection<Course> courses = session.loadByProperty(Course.class,new Property<String, Object>("name","CompSci"));
        assertEquals(1,courses.size());
        assertEquals(course,courses.iterator().next());
    }
}
