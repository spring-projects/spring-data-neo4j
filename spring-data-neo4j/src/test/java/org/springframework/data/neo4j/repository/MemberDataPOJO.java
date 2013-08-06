/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.repository;

import org.springframework.data.neo4j.annotation.POJOResult;
import org.springframework.data.neo4j.annotation.ResultColumn;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.model.Person;

import java.io.Serializable;
import java.util.Set;


@POJOResult
public class MemberDataPOJO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ResultColumn("collect(team)")
    private Set<Group> teams;

    @ResultColumn("boss")
    private Person boss;

    @ResultColumn("someonesAge")
    private int anInt;

    @ResultColumn("someonesName")
    private String aName;

    public Set<Group> getTeams() {
        return teams;
    }

    public void setTeams(Set<Group> teams) {
        this.teams = teams;
    }

    public Person getBoss() {
        return boss;
    }

    public void setBoss(Person boss) {
        this.boss = boss;
    }

    public int getAnInt() {
        return anInt;
    }

    public void setAnInt(int anInt) {
        this.anInt = anInt;
    }

    public String getAName() {
        return aName;
    }

    public void setAName(String name) {
        this.aName = name;
    }
}
