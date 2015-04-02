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
package org.neo4j.ogm.session.request.strategy;

import java.util.Collection;

import org.neo4j.ogm.cypher.statement.ParameterisedStatement;
import org.neo4j.ogm.session.Utils;

/**
 * @author Luanne Misquitta
 */
public class DeleteNodeStatements implements DeleteStatements{

    public ParameterisedStatement delete(Long id) {
        return new ParameterisedStatement("MATCH (n) WHERE id(n) = { id } OPTIONAL MATCH (n)-[r]-() DELETE r, n", Utils.map("id", id));
    }

    public ParameterisedStatement deleteAll(Collection<Long> ids) {
        return new ParameterisedStatement("MATCH (n) WHERE id(n) in { ids } OPTIONAL MATCH (n)-[r]-() DELETE r, n", Utils.map("ids", ids));
    }

    public ParameterisedStatement purge() {
        return new ParameterisedStatement("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE r, n", Utils.map());
    }

    public ParameterisedStatement deleteByType(String label) {
        return new ParameterisedStatement(String.format("MATCH (n:`%s`) OPTIONAL MATCH (n)-[r]-() DELETE r, n", label), Utils.map());
    }
}
