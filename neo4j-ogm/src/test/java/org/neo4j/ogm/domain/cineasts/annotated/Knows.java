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

package org.neo4j.ogm.domain.cineasts.annotated;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

import java.util.Date;

/**
 * @author Luanne Misquitta
 */
@RelationshipEntity
public class Knows {

    Long id;

    @StartNode
    private Actor firstActor;
    @EndNode
    private Actor secondActor;
    private Date since;

    public Actor getFirstActor() {
        return firstActor;
    }

    public void setFirstActor(Actor firstActor) {
        this.firstActor = firstActor;
    }

    public Actor getSecondActor() {
        return secondActor;
    }

    public void setSecondActor(Actor secondActor) {
        this.secondActor = secondActor;
    }

    public Date getSince() {
        return since;
    }

    public void setSince(Date since) {
        this.since = since;
    }
}
