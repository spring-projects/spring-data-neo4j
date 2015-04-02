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

package org.neo4j.ogm.domain.convertible.enums;

import java.util.List;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class Person {

    private Long id;
    private String name;
    private Gender gender;
    private List<Education> completedEducation;
    private Education[] inProgressEducation;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public List<Education> getCompletedEducation() {
        return completedEducation;
    }

    public void setCompletedEducation(List<Education> completedEducation) {
        this.completedEducation = completedEducation;
    }

    public Education[] getInProgressEducation() {
        return inProgressEducation;
    }

    public void setInProgressEducation(Education[] inProgressEducation) {
        this.inProgressEducation = inProgressEducation;
    }
}
