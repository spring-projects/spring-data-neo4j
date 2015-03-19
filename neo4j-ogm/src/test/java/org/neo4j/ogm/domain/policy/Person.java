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

/**
 * @author Mark Angrish
 */
public class Person extends DomainObject {

    private Set<Policy> influenced = new HashSet<>();

    @Relationship(type="WRITES_POLICY")
    private Set<Policy> written = new HashSet<>();

    public Person(String name) {
        setName(name);
    }

    public Set<Policy> getInfluenced() {
        return influenced;
    }

    public void setInfluenced(Set<Policy> influenced) {
        this.influenced = influenced;
    }

    public Set<Policy> getWritten() {
        return written;
    }

    public void setWritten(Set<Policy> written) {
        this.written = written;
    }
}
