package org.springframework.data.neo4j.repository.support;

import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.repository.GraphRepositoryImpl;
import org.springframework.data.neo4j.repository.query.GraphQueryLookupStrategy;
import org.springframework.data.repository.core.EntityInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;

import java.io.Serializable;

public class GraphRepositoryFactory extends RepositoryFactorySupport {

    private final Session session;

    public GraphRepositoryFactory(Session session) {
        this.session = session;
    }

    @Override
    public <T, ID extends Serializable> EntityInformation<T, ID> getEntityInformation(Class<T> type) {
        return new GraphEntityInformation(type, session);
    }

    @Override
    protected Object getTargetRepository(RepositoryMetadata repositoryMetadata) {
        return new GraphRepositoryImpl<>(repositoryMetadata.getDomainType(), session);
    }

    @Override
    protected Class<?> getRepositoryBaseClass(RepositoryMetadata repositoryMetadata) {
        return GraphRepositoryImpl.class;
    }

    @Override
    protected QueryLookupStrategy getQueryLookupStrategy(QueryLookupStrategy.Key key,
                                                         EvaluationContextProvider evaluationContextProvider) {
        return new GraphQueryLookupStrategy(session, key, evaluationContextProvider);
    }

}
