/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */


package org.springframework.data.neo4j.examples.restaurants.domain;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.conversion.LocationConverter;

public class Restaurant {

    @GraphId
    private Long id;
    private String name;
    @Convert(LocationConverter.class)
    private Point location;
    private int zip;

    public Restaurant() {
    }

    public Restaurant(String name, Point location, int zip) {
        this.name = name;
        this.location = location;
        this.zip = zip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public int getZip() {
        return zip;
    }

    public void setZip(int zip) {
        this.zip = zip;
    }

}
