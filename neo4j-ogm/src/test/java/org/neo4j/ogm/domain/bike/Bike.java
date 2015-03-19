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

package org.neo4j.ogm.domain.bike;

import java.util.List;

public class Bike {

    private String[] colours;
    private Long id;
    private List<Wheel> wheels;
    private Frame frame;
    private Saddle saddle;
    private String brand;

    public String getBrand()
    {
        return brand;
    }

    public void setBrand(String brand)
    {
        this.brand = brand;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String[] getColours() {
        return colours;
    }

    public List<Wheel> getWheels() {
        return wheels;
    }

    public void setWheels(List<Wheel> wheels) {
        this.wheels = wheels;
    }

    public void setSaddle(Saddle saddle)
    {
        this.saddle = saddle;
    }

    public Frame getFrame() {
        return frame;
    }

    public Saddle getSaddle() {
        return saddle;
    }

    public void setColours(String[] colours) {
        this.colours = colours;
    }

    public void setFrame(Frame frame) {
        this.frame = frame;
    }
}
