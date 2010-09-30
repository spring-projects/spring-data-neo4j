package org.springframework.datastore.graph.neo4j.fieldaccess;

import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.neo4j.support.GraphDatabaseContext;

import java.util.Arrays;
import java.util.Collection;

/**
* @author Michael Hunger
* @since 30.09.2010
*/
class NodeDelegatingFieldAccessorFactory extends DelegatingFieldAccessorFactory<NodeBacked> {
    public NodeDelegatingFieldAccessorFactory(GraphDatabaseContext graphDatabaseContext) {
        super(graphDatabaseContext);
    }

    @Override
    protected Collection<FieldAccessorListenerFactory<?>> createListenerFactories() {
        return Arrays.<FieldAccessorListenerFactory<?>>asList(
                new IndexingNodePropertyFieldAccessorListenerFactory(new PropertyFieldAccessorFactory(), new ConvertingNodePropertyFieldAccessorFactory())
        );
    }

    @Override
    protected Collection<? extends FieldAccessorFactory<?>> createAccessorFactories() {
        return Arrays.<FieldAccessorFactory<?>>asList(
                new IdFieldAccessorFactory(),
                new TransientFieldAccessorFactory(),
                new PropertyFieldAccessorFactory(),
                new ConvertingNodePropertyFieldAccessorFactory(),
                new SingleRelationshipFieldAccessorFactory(),
                new OneToNRelationshipFieldAccessorFactory(),
                new ReadOnlyOneToNRelationshipFieldAccessorFactory(),
                new TraversalFieldAccessorFactory(),
                new OneToNRelationshipEntityFieldAccessorFactory()
        );
    }
}
