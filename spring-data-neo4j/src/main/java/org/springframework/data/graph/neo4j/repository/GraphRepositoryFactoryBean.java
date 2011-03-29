package org.springframework.data.graph.neo4j.repository;

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.data.repository.support.RepositoryFactorySupport;
import org.springframework.data.repository.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.util.Assert;

/**
 * @author mh
 * @since 28.03.11
 */
public class GraphRepositoryFactoryBean<S extends PropertyContainer, R extends CRUDGraphRepository<S,T>, T extends GraphBacked<S>>
        extends TransactionalRepositoryFactoryBeanSupport<R, T, Long> {

    private GraphDatabaseContext graphDatabaseContext;

    public void setGraphDatabaseContext(GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }


    @Override
    protected RepositoryFactorySupport doCreateRepositoryFactory() {

        return createRepositoryFactory(graphDatabaseContext);
    }


    protected RepositoryFactorySupport createRepositoryFactory(GraphDatabaseContext graphDatabaseContext) {

        return new GraphRepositoryFactory(graphDatabaseContext);
    }

    @Override
    public void afterPropertiesSet() {
        Assert.notNull(graphDatabaseContext, "GraphDatabaseContext must not be null!");
        super.afterPropertiesSet();
    }
}