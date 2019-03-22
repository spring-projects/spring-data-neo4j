/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.annotation.relatedto;

import org.springframework.data.neo4j.annotation.IdentifiableEntity;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

@NodeEntity
public class CareerProfile extends IdentifiableEntity {
    private String name;

    @RelatedTo(enforceTargetType = true)
    private Set<Experience> experience = new HashSet<Experience>();

    @RelatedTo(type = "experience", enforceTargetType = true)
    private Set<Job> jobs = new HashSet<Job>();

    public CareerProfile() {
    }

    public CareerProfile(String name) {
        this.name = name;
    }

    public void addExperience(Experience... experience) {
        this.experience.addAll(asList(experience));
    }

    public void addJobs(Job... jobs) {
        this.jobs.addAll(asList(jobs));
    }

    public Set<Experience> getExperience() {
        return experience;
    }

    public Set<Job> getJobs() {
        return jobs;
    }
}
