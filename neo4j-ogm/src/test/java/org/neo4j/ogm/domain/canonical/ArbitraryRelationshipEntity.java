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

package org.neo4j.ogm.domain.canonical;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;

/**
 * NB: this isn't actually used to save anything at the time of writing; it's just so we can test meta-data resolution for
 * relationship entities.
 *
 * @author Adam George
 */
@RelationshipEntity(type = "MEMBER_OF")
public class ArbitraryRelationshipEntity {

    @StartNode
    Start start;

    @EndNode
    End end;

    public static class Start {
        Long id;
        String name;
    }

    public static class End {
        Long id;
        String name;
    }
}
