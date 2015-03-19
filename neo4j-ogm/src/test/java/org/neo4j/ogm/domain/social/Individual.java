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

package org.neo4j.ogm.domain.social;

import java.util.List;
import java.util.Vector;

/**
 * Arbitrary POJO used to test mapping code.
 *
 * @author Adam George
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
