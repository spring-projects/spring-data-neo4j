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

import java.util.Collections;
import java.util.Set;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.Relationship;

import com.fasterxml.jackson.core.type.TypeReference;

@NodeEntity
public class Team {

    @GraphId
    private Long id;
    private String name;
    @Relationship(type = "PLAYS_FOR", direction = Relationship.INCOMING)
    private Set<Cricketer> teamPlayers = Collections.emptySet();
    private TypeReference<Integer> utterRubbish; // completely irrelevant item from non-local domain

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Cricketer> getTeamPlayers() {
        return teamPlayers;
    }

    public void setTeamPlayers(Set<Cricketer> teamPlayers) {
        this.teamPlayers = teamPlayers;
    }

    public TypeReference<Integer> getUtterRubbish() {
        return utterRubbish;
    }

    public void setUtterRubbish(TypeReference<Integer> utterRubbish) {
        this.utterRubbish = utterRubbish;
    }

}
