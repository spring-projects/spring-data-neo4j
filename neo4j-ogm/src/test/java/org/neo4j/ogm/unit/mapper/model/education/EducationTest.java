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

package org.neo4j.ogm.unit.mapper.model.education;

import org.junit.Test;
import org.neo4j.ogm.domain.education.Course;
import org.neo4j.ogm.domain.education.Student;
import org.neo4j.ogm.domain.education.Teacher;
import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.SessionFactory;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Vince Bickers
 */
public class EducationTest {

    private static final SessionFactory sessionFactory = new SessionFactory("org.neo4j.ogm.domain.education");
    private static final Neo4jSession session = (Neo4jSession) sessionFactory.openSession("dummy-url");

    @Test
    public void testTeachers() throws Exception {

        Map<String, Teacher> teachers = loadTeachers();

        Teacher mrThomas = teachers.get("Mr Thomas");
        Teacher mrsRoberts = teachers.get("Mrs Roberts");
        Teacher missYoung = teachers.get("Miss Young");

        checkCourseNames(mrThomas, "Maths", "English", "Physics");
        checkCourseNames(mrsRoberts, "English", "History", "PE");
        checkCourseNames(missYoung, "History", "Geography", "Philosophy and Ethics");

    }

    @Test
    public void testFetchCoursesTaughtByAllTeachers() throws Exception {

        Map<String, Teacher> teachers = loadTeachers();  // note: idempotent!

        Teacher mrThomas = teachers.get("Mr Thomas");
        Teacher mrsRoberts = teachers.get("Mrs Roberts");
        Teacher missYoung = teachers.get("Miss Young");

        checkCourseNames(mrThomas, "Maths", "English", "Physics");
        checkCourseNames(mrsRoberts, "English", "History", "PE");
        checkCourseNames(missYoung, "History", "Geography", "Philosophy and Ethics");

        // this response is for an imagined request: "match p = (c:COURSE)--(o) where id(c) in [....] RETURN p"
        // i.e. we have a set of partially loaded courses attached to our teachers which we now want to
        // hydrate by getting all their relationships
        hydrateCourses(teachers.values());


        Set<Course> courses = new HashSet<>();
        for (Teacher teacher : teachers.values()) {
            for (Course course : teacher.getCourses()) {
                if (!courses.contains(course)) {
                    List<Student> students = course.getStudents();
                    if (course.getName().equals("Maths")) checkMaths(students);
                    else if (course.getName().equals("Physics")) checkPhysics(students);
                    else if (course.getName().equals("Philosophy and Ethics")) checkPhilosophyAndEthics(students);
                    else if (course.getName().equals("PE")) checkPE(students);
                    else if (course.getName().equals("History")) checkHistory(students);
                    else if (course.getName().equals("Geography")) checkGeography(students);
                    else checkEnglish(students);
                    courses.add(course);
                }
            }
        }
        assertEquals(7, courses.size());
    }

    private void test(long hash, List<Student> students) {
        for (Student student : students) {
            hash-= student.getId();
        }
        assertEquals(0, hash);
    }

    // all students study english
    private void checkEnglish(List<Student> students) {
        long hash = 0;
        for (int i = 101; i < 127; i++) {
            hash+=i;
        }
        test(hash, students);
    }


    // all students whose ids modulo 100 are prime study geography. 1 is not considered prime
    private void checkGeography(List<Student> students) {
        long hash = 102 + 103 + 105 + 107 + 111 + 113 + 117 + 119 + 123;

        test(hash, students);
    }

    // all students with even ids study history
    private void checkHistory(List<Student> students) {
        long hash = 0;
        for (int i = 102; i < 127; i+=2) {
            hash+=i;
        }
        test(hash, students);
    }

    // every 3rd student studies PE
    private void checkPE(List<Student> students) {
        long hash = 0;
        for (int i = 103; i < 127; i+=3) {
            hash+=i;
        }
        test(hash, students);
    }

    // all students are deep thinkers
    private void checkPhilosophyAndEthics(List<Student> students) {
        long hash = 0;
        for (int i = 101; i < 127; i++) {
            hash+=i;
        }
        test(hash, students);
    }

    // all students with odd ids study physics
    private void checkPhysics(List<Student> students) {
        long hash = 0;
        for (int i = 101; i < 127; i+=2) {
            hash+=i;
        }
        test(hash, students);
    }

    // all students study maths
    private void checkMaths(List<Student> students) {
        long hash = 0;
        for (int i = 101; i < 127; i++) {
            hash+=i;
        }
        test(hash, students);
    }

    private Map<String, Teacher> loadTeachers() throws Exception {

        Map<String, Teacher> teachers = new HashMap<>();

        session.setRequest(new TeacherRequest());
        Collection<Teacher> teacherList = session.loadAll(Teacher.class);

        for (Teacher teacher : teacherList ) {
            teachers.put(teacher.getName(), teacher);
        }

        return teachers;
    }

    // when we hydrate a set of things that are previously loaded we don't need to create them afresh
    // - the object map of the existing objects is simply extended with new data.
    // TODO: there is NO TEST asserting this.
    private void hydrateCourses(Collection<Teacher> teachers) throws Exception {

        // normally we'd use the default request handler, but for
        session.setRequest(new CourseRequest());

        Set<Long> courses= new HashSet<>();
        for (Teacher teacher : teachers) {
            for (Course course: teacher.getCourses()) {
                courses.add(course.getId());
            }
        }

        session.loadAll(Course.class, courses);
    }

    private void checkCourseNames(Teacher teacher, String... courseNames) {

        int n = courseNames.length;
        List<String> test = Arrays.asList(courseNames);

        System.out.println(teacher.getName() + " courses: ");
        for (Course course : teacher.getCourses()) {
            System.out.println(course.getName() + ": " + course.hashCode());
            if (test.contains(course.getName())) {
                n--;
            }
        }
        System.out.println("---------------------");
        assertEquals(0, n);
    }
}
