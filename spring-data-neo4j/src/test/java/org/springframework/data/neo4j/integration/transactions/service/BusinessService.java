/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */
package org.springframework.data.neo4j.integration.transactions.service;

import org.neo4j.ogm.session.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.template.Neo4jTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * @author: Vince Bickers
 */
@Component
public class BusinessService {

    private static final Logger log = LoggerFactory.getLogger(BusinessService.class);

    @Autowired
    private Session session;

    @Transactional
    public void successMethod() {
        insertNode();
    }

    @Transactional
    public void failMethod() {
        insertNode();
        throw new RuntimeException("Deliberate to force rollback of entire transaction");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void forceMethod() {
        insertNode();
    }

    private void insertNode() {
        new Neo4jTemplate(session).execute("CREATE node");
    }

    public Iterable<Map<String, Object>> loadNodes() {
        return new Neo4jTemplate(session).query("MATCH n RETURN n", new HashMap<String, Object>());
    }

    public void purge() {
        new Neo4jTemplate(session).execute("MATCH n DELETE n");

    }
}
