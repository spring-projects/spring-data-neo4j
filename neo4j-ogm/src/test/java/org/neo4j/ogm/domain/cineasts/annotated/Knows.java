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
package org.neo4j.ogm.domain.cineasts.annotated;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

import java.util.Date;

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
