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

package org.neo4j.ogm.domain.policy;

import org.neo4j.ogm.annotation.Relationship;

import java.util.HashSet;
import java.util.Set;

public class Policy extends DomainObject {

    private Set<Person> influencers = new HashSet<>();

    @Relationship(type="WRITES_POLICY", direction="INCOMING")
    private Set<Person> writers = new HashSet<>();

    public Policy(String name) {
        setName(name);
    }

    public Set<Person> getInfluencers() {
        return influencers;
    }

    public void setInfluencers(Set<Person> influencers) {
        this.influencers = influencers;
    }

    public Set<Person> getWriters() {
        return writers;
    }

    public void setWriters(Set<Person> writers) {
        this.writers = writers;
    }
}
