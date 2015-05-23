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

package org.neo4j.ogm.domain.ingredients;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

/**
 * @author Luanne Misquitta
 */
@RelationshipEntity(type = "PAIRS_WITH")
public class Pairing {

    Long id;

    @StartNode
    private Ingredient first;
    @EndNode
    private Ingredient second;
    private String affinity;


    public Pairing() {
    }

    public Ingredient getFirst() {
        return first;
    }

    public void setFirst(Ingredient first) {
        this.first = first;
    }

    public Ingredient getSecond() {
        return second;
    }

    public void setSecond(Ingredient second) {
        this.second = second;
    }

    public String getAffinity() {
        return affinity;
    }

    public void setAffinity(String affinity) {
        this.affinity = affinity;
    }
}
