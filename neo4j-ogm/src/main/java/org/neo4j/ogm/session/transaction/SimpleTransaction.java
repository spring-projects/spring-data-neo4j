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

package org.neo4j.ogm.session.transaction;

import org.neo4j.ogm.cypher.compiler.CypherContext;
import org.neo4j.ogm.mapper.MappedRelationship;
import org.neo4j.ogm.mapper.MappingContext;
import org.neo4j.ogm.mapper.TransientRelationship;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vince Bickers
 */
public class SimpleTransaction implements Transaction {

    private final Logger logger = LoggerFactory.getLogger(Transaction.class);
    private final MappingContext mappingContext;
    private final String url;
    private final boolean autocommit;

    private final List<CypherContext> contexts;

    private Status status = Status.OPEN;

    public SimpleTransaction(MappingContext mappingContext, String url) {
        this.mappingContext = mappingContext;
        this.url = url;
        this.autocommit = url.endsWith("/commit");
        this.contexts = new ArrayList<>();
    }

    public final void append(CypherContext context) {
        logger.debug("Appending transaction context " + context);
        if (status == Status.OPEN || status == Status.PENDING) {
            contexts.add(context);
            status = Status.PENDING;
            if (autocommit) {
                commit();
            }
        } else {
            throw new TransactionException("Transaction is no longer open. Cannot accept new operations");
        }
    }

    public final String url() {
        return url;
    }

    public void rollback() {
        logger.info("rollback invoked");
        if (status == Status.OPEN || status == Status.PENDING) {
            contexts.clear();
            status = Status.ROLLEDBACK;
        } else {
            throw new TransactionException("Transaction is no longer open. Cannot rollback");
        }
    }

    public void commit() {
        logger.info("commit invoked");
        if (status == Status.OPEN || status == Status.PENDING) {
            synchroniseSession();
            status = Status.COMMITTED;
        } else {
            throw new TransactionException("Transaction is no longer open. Cannot commit");
        }
    }

    public final Status status() {
        return status;
    }

    public void close() {
        status = Status.CLOSED;
    }

    private void synchroniseSession()  {

        for (CypherContext cypherContext : contexts) {

            logger.info("Synchronizing transaction context " + cypherContext + " with session context");

            for (Object o : cypherContext.log())  {
                logger.debug("checking cypher context object: " + o);
                if (o instanceof MappedRelationship) {
                    MappedRelationship mappedRelationship = (MappedRelationship) o;
                    if (mappedRelationship.isActive()) {
                        logger.debug("activating (${})-[:{}]->(${})", mappedRelationship.getStartNodeId(), mappedRelationship.getRelationshipType(), mappedRelationship.getEndNodeId());
                        mappingContext.registerRelationship((MappedRelationship) o);
                    } else {
                        logger.debug("de-activating (${})-[:{}]->(${})", mappedRelationship.getStartNodeId(), mappedRelationship.getRelationshipType(), mappedRelationship.getEndNodeId());
                        mappingContext.mappedRelationships().remove(mappedRelationship);
                    }
                } else if (!(o instanceof TransientRelationship)) {
                    logger.debug("remembering " + o);
                    mappingContext.remember(o);
                }
            }
            logger.debug("number of objects: " + cypherContext.log().size());
        }

        contexts.clear();
    }


}