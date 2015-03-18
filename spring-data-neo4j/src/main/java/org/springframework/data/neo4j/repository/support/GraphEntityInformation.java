package org.springframework.data.neo4j.repository.support;

import org.neo4j.ogm.session.Session;
import org.springframework.data.repository.core.support.AbstractEntityInformation;

import java.io.Serializable;

/**
 * Created by markangrish on 13/01/2015.
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
