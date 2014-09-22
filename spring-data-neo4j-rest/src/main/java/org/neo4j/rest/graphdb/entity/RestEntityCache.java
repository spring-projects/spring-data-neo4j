package org.neo4j.rest.graphdb.entity;

import org.neo4j.kernel.impl.cache.LruCache;
import org.neo4j.rest.graphdb.RestAPI;

/**
 * @author mh
 * @since 21.09.14
 */
public class RestEntityCache {

    private LruCache<Long,RestNode> lruCache = new LruCache<>("RestNode",10000);

    private final RestAPI restAPI;

    public RestEntityCache(RestAPI restAPI) {
        this.restAPI = restAPI;
    }

    public RestNode addToCache(RestNode node) {
        if (node == null) return null;
        long id = node.getId();
        if (id != -1) {
            RestNode existing = lruCache.get(id);
            if (existing !=null) {
                if (existing != node) existing.updateFrom(node, restAPI);
                return existing;
            } else {
                lruCache.put(id, node);
            }
        }
        return node;
    }

    public RestNode getNode(long id) {
        return lruCache.get(id);
    }

    public void remove(long id) {
        lruCache.remove(id);
    }
}
