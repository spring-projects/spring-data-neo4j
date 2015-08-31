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

import javax.transaction.Status;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.neo4j.helpers.collection.MapUtil.map;

public class RemoteCypherTransaction implements Transaction {

    private final List<TransactionFinishListener> listeners = new ArrayList<>();

    int status = Status.STATUS_NO_TRANSACTION;
    boolean success, failure;
    CypherTransaction tx;
    AtomicInteger innerCounter = new AtomicInteger(1);

    @Override
    public String toString() {
        return "RemoteCypherTransaction@"+System.identityHashCode(this)+"{" +
                "status=" + status +
                ", success=" + success +
                ", failure=" + failure +
                ", tx=" + tx +
                ", innerCounter=" + innerCounter +
                '}';
    }

    public void registerListener(TransactionFinishListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public RemoteCypherTransaction(CypherTransaction tx) {
        this.tx = tx;
        status = Status.STATUS_ACTIVE;
    }

    public void beginInner() {
        if (status == Status.STATUS_ACTIVE || status == Status.STATUS_COMMITTING || status == Status.STATUS_MARKED_ROLLBACK) {
            innerCounter.incrementAndGet();
        } else {
            throw new IllegalStateException("Can't begin nested tx on non-active transaction status is " + status + " tx " + this);
        }
    }

    public void success() {
        this.success = true;
        if (!failure) status = Status.STATUS_COMMITTING; // ???
    }

    public void finish() {
        close();
    }

    @Override
    public void close() {
        if (tx() != null && innerCounter.decrementAndGet() > 0) {
            return;
        }
        try {
            if (success && !failure) {
                tx().commit();
                status = Status.STATUS_COMMITTED;
            }
            else {
                if (tx()!=null) tx().rollback();
                status = Status.STATUS_ROLLEDBACK;
            }
        } finally {
            tx = null;
            notifyFinish();
        }
    }

    private void notifyFinish() {
        for (TransactionFinishListener listener : listeners) {
            if (status == Status.STATUS_COMMITTED) listener.comitted();
            else listener.rolledBack();
        }
        listeners.clear();
    }

    private CypherTransaction tx() {
        if (tx == null && !failure) throw new IllegalStateException("No transaction active");
        return tx;
    }

    public void failure() {
        this.failure = true;
        status = Status.STATUS_MARKED_ROLLBACK;
    }

    @Override
    public Lock acquireWriteLock(PropertyContainer pc) {
        if (pc instanceof Node) {
            tx().send("MATCH (n) WHERE id(n) = {id} REMOVE n.` lock property `", map("id", ((Node) pc).getId()), false);
        }
        if (pc instanceof Relationship) {
            tx().send("START r=rel({id}) REMOVE r.` lock property `", map("id", ((Relationship) pc).getId()), false);
        }
        return new Lock() { public void release() { } }; // release at commit
    }

    @Override
    public Lock acquireReadLock(PropertyContainer propertyContainer) {
        return null;
    }

    public int getStatus() {
        return status;
    }


    public CypherTransaction getTransaction() {
        return tx;
    }

    public boolean isActive() {
        return tx != null;
    }

    public void terminate() {
        failure();
        close();
    }
}
