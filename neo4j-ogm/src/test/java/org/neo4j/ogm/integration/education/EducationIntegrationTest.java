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


import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.ogm.domain.education.Course;
import org.neo4j.ogm.domain.education.School;
import org.neo4j.ogm.domain.education.Student;
import org.neo4j.ogm.domain.education.Teacher;
import org.neo4j.ogm.integration.InMemoryServerTest;
import org.neo4j.ogm.model.Property;
import org.neo4j.ogm.session.SessionFactory;

/**
 * @author Luanne Misquitta
 */
public class EducationIntegrationTest extends InMemoryServerTest {

    @Before
    public void init() throws IOException {
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
        assertEquals(1,courses.iterator().next().getStudents().size());
    }

    /**
     * @see DATAGRAPH-595
     */
    @Test
    public void loadingASchoolWithNegativeDepthShouldLoadAllConnectedEntities() {
        //Create students, teachers, courses and a school
        School hogwarts = new School("Hogwarts");

        Student harry = new Student("Harry Potter");
        Student ron = new Student("Ron Weasley");
        Student hermione = new Student("Hermione Granger");

        Course transfiguration = new Course("Transfiguration");
        transfiguration.setStudents(Arrays.asList(harry, hermione, ron));

        Course potions = new Course("Potions");
        potions.setStudents(Arrays.asList(ron, hermione));

        Course dark = new Course("Defence Against The Dark Arts");
        dark.setStudents(Collections.singletonList(harry));

        Teacher minerva = new Teacher("Minerva McGonagall");
        minerva.setCourses(Collections.singletonList(transfiguration));
        minerva.setSchool(hogwarts);

        Teacher severus = new Teacher("Severus Snape");
        severus.setCourses(Arrays.asList(potions, dark));
        severus.setSchool(hogwarts);

        hogwarts.setTeachers(Arrays.asList(minerva, severus));
        session.save(hogwarts);

        session.clear();
        //Load the school with depth -1
        hogwarts = session.load(School.class,hogwarts.getId(),-1);
        assertEquals(2, hogwarts.getTeachers().size());
        for(Teacher teacher : hogwarts.getTeachers()) {
            if(teacher.getName().equals("Severus Snape")) {
                assertEquals(2, teacher.getCourses().size());
                for(Course course : teacher.getCourses()) {
                    if(course.getName().equals("Potions")) {
                        assertEquals(2, course.getStudents().size());
                    }
                    else {
                        assertEquals(1, course.getStudents().size());
                    }
                }
            }
            else {
                assertEquals(1, teacher.getCourses().size());
                assertEquals(3, teacher.getCourses().get(0).getStudents().size());
            }
        }
    }
}
