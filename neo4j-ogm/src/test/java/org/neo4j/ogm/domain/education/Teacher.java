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

package org.neo4j.ogm.domain.education;

import java.util.List;

/**
 * @author Vince Bickers
 */
public class Teacher {

    private String name;
    private List<Course> courses;
    private Long id;
    private School school;

    public Teacher() {}

    public Teacher(String name) {
        setName(name);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // @Lazy
    public List<Course> getCourses() {
        return courses;
    }

    public void setCourses(List<Course> courses) {
        // persistable?
        this.courses = courses;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {

        if (this.school != null) {
            this.school.getTeachers().remove(this);
        }

        this.school = school;

        if (this.school != null) {
            this.school.getTeachers().add(this);
        }

    }

}
