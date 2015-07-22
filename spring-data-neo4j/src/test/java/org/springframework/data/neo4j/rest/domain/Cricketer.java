/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and licence terms.  Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's licence, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.rest.domain;

import java.math.BigDecimal;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

@NodeEntity
public class Cricketer {

    @GraphId
    private Long id;
    private String name;
    @Relationship(type = "PLAYS_FOR")
    private Team cricketTeam;
    private BigDecimal battingAverage;

    Cricketer() {}

    public Cricketer(String name, BigDecimal battingAverage, Team team) {
        this.name = name;
        this.battingAverage = battingAverage;
        this.cricketTeam = team;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Team getCricketTeam() {
        return cricketTeam;
    }

    public BigDecimal getBattingAverage() {
        return battingAverage;
    }

}
