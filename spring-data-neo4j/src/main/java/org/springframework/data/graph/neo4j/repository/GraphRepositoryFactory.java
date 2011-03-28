package org.springframework.data.graph.neo4j.repository;

import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.support.EntityInformation;
import org.springframework.data.repository.support.RepositoryFactorySupport;
import org.springframework.data.repository.support.RepositoryMetadata;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.lang.reflect.Method;

import static org.springframework.core.GenericTypeResolver.resolveTypeArguments;

/**
 * @author mh
 * @since 28.03.11
 */
public class GraphRepositoryFactory extends RepositoryFactorySupport {


    private final GraphDatabaseContext graphDatabaseContext;

    public GraphRepositoryFactory(GraphDatabaseContext graphDatabaseContext) {

        Assert.notNull(graphDatabaseContext);
        this.graphDatabaseContext = graphDatabaseContext;
    }


    /*
     * (non-Javadoc)
     *
     * @see
     * org.springframework.data.repository.support.RepositoryFactorySupport#
     * getTargetRepository(java.lang.Class)
     */
    @Override
    protected Object getTargetRepository(RepositoryMetadata metadata) {
        return getTargetRepository(metadata, graphDatabaseContext);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Object getTargetRepository(RepositoryMetadata metadata, GraphDatabaseContext graphDatabaseContext) {
        Class<?> repositoryInterface = metadata.getRepositoryInterface();
        Class<?> type = metadata.getDomainClass();
        GraphEntityInformation entityInformation = (GraphEntityInformation)getEntityInformation(type);

        if (entityInformation.isNodeEntity()) {
            return new DefaultNodeGraphRepository(type,graphDatabaseContext);
        } else {
            return new DefaultRelationshipGraphRepository(type,graphDatabaseContext);
        }
    }

    private static Class<?> getDomainClass(Class repositoryInterface) {
        Class<?>[] arguments = resolveTypeArguments(repositoryInterface, Repository.class);
        return arguments == null ? null : arguments[0];
    }


    @Override
    protected Class<?> getRepositoryBaseClass(Class<?> repositoryInterface) {
        Class<?> domainClass = getDomainClass(repositoryInterface);
        if (domainClass.isAnnotationPresent(NodeEntity.class)) {
            return DefaultNodeGraphRepository.class;
        } else {
            return DefaultRelationshipGraphRepository.class;
        }
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T, ID extends Serializable> EntityInformation<T, ID> getEntityInformation(Class<T> type) {
        return new GraphMetamodelEntityInformation(type,graphDatabaseContext);
    }


    @Override
    protected QueryLookupStrategy getQueryLookupStrategy(QueryLookupStrategy.Key key) {
        return new QueryLookupStrategy(){
            @Override
            public RepositoryQuery resolveQuery(Method method, Class<?> domainClass) {
                return null;
            }
        };
    }
}