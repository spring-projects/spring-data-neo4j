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
 * @author Luanne Misquitta
 */
public class Individual {

    private Long id;
    private String name;
    private int age;
    private float bankBalance;
    private byte code;
    private Integer numberOfPets;
    private Float distanceFromZoo;
    private Byte numberOfShoes;

    private List<Individual> friends;
    private int[] primitiveIntArray;
    private byte[] primitiveByteArray;
    public float[] primitiveFloatArray;
    public Integer[] integerArray;
    public Float[] floatArray;
    public List<Integer> integerCollection;
    private List<Float> floatCollection;
    private List<Byte> byteCollection;

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

    public float getBankBalance() {
        return bankBalance;
    }

    public void setBankBalance(float bankBalance) {
        this.bankBalance = bankBalance;
    }

    public byte getCode() {
        return code;
    }

    public void setCode(byte code) {
        this.code = code;
    }

    public Integer getNumberOfPets() {
        return numberOfPets;
    }

    public void setNumberOfPets(Integer numberOfPets) {
        this.numberOfPets = numberOfPets;
    }

    public Float getDistanceFromZoo() {
        return distanceFromZoo;
    }

    public void setDistanceFromZoo(Float distanceFromZoo) {
        this.distanceFromZoo = distanceFromZoo;
    }

    public Byte getNumberOfShoes() {
        return numberOfShoes;
    }

    public void setNumberOfShoes(Byte numberOfShoes) {
        this.numberOfShoes = numberOfShoes;
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

    public List<Float> getFloatCollection() {
        return floatCollection;
    }

    public void setFloatCollection(List<Float> floatCollection) {
        this.floatCollection = floatCollection;
    }

    public List<Byte> getByteCollection() {
        return byteCollection;
    }

    public void setByteCollection(List<Byte> byteCollection) {
        this.byteCollection = byteCollection;
    }
}
