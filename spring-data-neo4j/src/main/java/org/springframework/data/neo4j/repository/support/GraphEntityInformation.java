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

package org.springframework.data.neo4j.repository.support;

import org.neo4j.ogm.session.Session;
import org.springframework.data.repository.core.support.AbstractEntityInformation;

import java.io.Serializable;

/**
 * @author Mark Angrish
 */
public class GraphEntityInformation<ID extends Serializable, T> extends AbstractEntityInformation<T, Long> {
    private final Session session;

    public GraphEntityInformation(Class<T> type, Session session) {
        super(type);
        this.session = session;
    }

    @Override
    public Long getId(T entity) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Class<Long> getIdType() {
        return Long.class;
    }

}
