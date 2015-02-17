package org.springframework.data.neo4j.repository.query;

import org.neo4j.ogm.session.Session;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.query.derived.DerivedGraphQueryMethod;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;

import java.lang.reflect.Method;

/**
 * Created by markangrish on 13/01/2015.
 */
public class GraphQueryLookupStrategy implements QueryLookupStrategy
{
    private final Session session;

    /**
     * Private constructor to prevent instantiation.
     */

    public GraphQueryLookupStrategy(Session session,
                                    QueryLookupStrategy.Key key,
                                    EvaluationContextProvider evaluationContextProvider)
    {
        this.session = session;
    }

    @Override
    public RepositoryQuery resolveQuery(Method method, RepositoryMetadata repositoryMetadata, NamedQueries namedQueries) {

        if (method.getAnnotation(Query.class) != null) {
            return new GraphQueryMethod(method, repositoryMetadata, session).createQuery();
        }

        return new DerivedGraphQueryMethod(method, repositoryMetadata, session).createQuery();
    }
}
