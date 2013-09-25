package org.springframework.data.neo4j.support.mapping;

import com.tinkerpop.gremlin.Tokens;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.ResourceIterable;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.helpers.collection.ClosableIterable;

import java.util.Iterator;

/**
 * @author Nicki Watt
 * @since 24-09-2013
 */
public class ResourceIterableClosableIterable implements ClosableIterable , ResourceIterable {

    private ResourceIterator iterator;

    public ResourceIterableClosableIterable(ResourceIterable<Node> resourceIterable) {
        this.iterator = resourceIterable.iterator();
    }

    @Override
    public void close() {
        iterator.close();
    }

    @Override
    public ResourceIterator iterator() {
        return iterator;
    }
};
