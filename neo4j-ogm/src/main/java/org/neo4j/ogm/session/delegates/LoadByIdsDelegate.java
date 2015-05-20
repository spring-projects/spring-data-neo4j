package org.neo4j.ogm.session.delegates;

import org.neo4j.ogm.cypher.query.Pagination;
import org.neo4j.ogm.cypher.query.Query;
import org.neo4j.ogm.cypher.query.SortOrder;
import org.neo4j.ogm.model.GraphModel;
import org.neo4j.ogm.session.Capability;
import org.neo4j.ogm.session.Neo4jSession;
import org.neo4j.ogm.session.request.strategy.QueryStatements;
import org.neo4j.ogm.session.response.Neo4jResponse;

import java.util.Collection;

/**
 * @author: Vince Bickers
 */
public class LoadByIdsDelegate implements Capability.LoadByIds {


    private final Neo4jSession session;

    public LoadByIdsDelegate(Neo4jSession session) {
        this.session = session;
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, SortOrder sortOrder, Pagination pagination, int depth) {

        String url = session.ensureTransaction().url();
        QueryStatements queryStatements = session.queryStatementsFor(type);

        Query qry = queryStatements.findAll(ids, depth)
                .setSortOrder(sortOrder)
                .setPage(pagination);

        try (Neo4jResponse<GraphModel> response = session.requestHandler().execute(qry, url)) {
            return session.responseHandler().loadAll(type, response);
        }
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids) {
        return loadAll(type, ids, new SortOrder(), null, 1);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, int depth) {
        return loadAll(type, ids, new SortOrder(), null, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, SortOrder sortOrder) {
        return loadAll(type, ids, sortOrder, null, 1);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, SortOrder sortOrder, int depth) {
        return loadAll(type, ids, sortOrder, null, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, Pagination paging) {
        return loadAll(type, ids, new SortOrder(), paging, 1);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, Pagination paging, int depth) {
        return loadAll(type, ids, new SortOrder(), paging, depth);
    }

    @Override
    public <T> Collection<T> loadAll(Class<T> type, Collection<Long> ids, SortOrder sortOrder, Pagination pagination) {
        return loadAll(type, ids, sortOrder, pagination, 1);
    }

}
