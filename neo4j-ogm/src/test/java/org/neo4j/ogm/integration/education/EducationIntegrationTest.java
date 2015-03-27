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

/**
 * @author Luanne Misquitta
 */
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
