/**
 * Copyright (c) 2002-2013 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.rest.graphdb.transaction;

import org.neo4j.graphdb.*;
import org.neo4j.rest.graphdb.query.CypherTransaction;

import static org.neo4j.helpers.collection.MapUtil.map;

public class RemoteCypherTransaction implements Transaction {

    boolean success, failure;
    ThreadLocal<CypherTransaction> tx;

    public RemoteCypherTransaction(ThreadLocal<CypherTransaction> tx) {
        this.tx = tx;
    }

    public void success() {
        this.success = true;
    }

    public void finish() {
        close();
    }

    @Override
    public void close() {
        try {
            if (success && !failure)
                tx().commit();
            else
                tx().rollback();
        } finally {
            tx.set(null);
        }
    }

    private CypherTransaction tx() {
        CypherTransaction cypherTransaction = tx.get();
        if (cypherTransaction == null) throw new IllegalStateException("No transaction active");
        return cypherTransaction;
    }

    public void failure() {
        this.failure = true;
    }

    @Override
    public Lock acquireWriteLock(PropertyContainer pc) {
        if (pc instanceof Node) {
            tx().send("MATCH (n) WHERE id(n) = {id} REMOVE n.` lock property `", map("id", ((Node) pc).getId()));
        }
        if (pc instanceof Relationship) {
            tx().send("START r=rel({id}) REMOVE r.` lock property `", map("id", ((Relationship) pc).getId()));
        }
        return new Lock() { public void release() { } }; // release at commit
    }

    @Override
    public Lock acquireReadLock(PropertyContainer propertyContainer) {
        return null;
    }
}
