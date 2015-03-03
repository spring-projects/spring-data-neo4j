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

package org.neo4j.ogm.domain.social;

import java.util.List;
import java.util.Vector;

/**
 * Arbitrary POJO used to test mapping code.
 */
public class Individual {

    private Long id;
    private String name;
    private int age;

    private List<Individual> friends;
    private int[] primitiveIntArray;
    private byte[] primitiveByteArray;

    private Vector<Double> favouriteRadioStations;

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

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public List<Individual> getFriends() {
        return friends;
    }

    public void setFriends(List<Individual> friends) {
        this.friends = friends;
    }

    public int[] getPrimitiveIntArray() {
        return primitiveIntArray;
    }

    public void setPrimitiveIntArray(int[] primitiveIntArray) {
        this.primitiveIntArray = primitiveIntArray;
    }

    public Vector<Double> getFavouriteRadioStations() {
        return favouriteRadioStations;
    }

    public void setFavouriteRadioStations(Vector<Double> fmFrequencies) {
        this.favouriteRadioStations = fmFrequencies;
    }

    public byte[] getPrimitiveByteArray() {
        return primitiveByteArray;
    }

    public void setPrimitiveByteArray(byte[] primitiveByteArray) {
        this.primitiveByteArray = primitiveByteArray;
    }
}
