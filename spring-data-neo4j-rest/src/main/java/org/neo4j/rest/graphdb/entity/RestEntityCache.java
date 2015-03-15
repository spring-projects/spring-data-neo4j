package org.neo4j.rest.graphdb.entity;

import org.neo4j.kernel.impl.cache.LruCache;
import org.neo4j.rest.graphdb.RestAPI;

/**
 * @author mh
 * @since 21.09.14
 */
public class RestEntityCache {

    private LruCache<Long,RestNode> lruNodeCache = new LruCache<>("RestNode",10000);
    private LruCache<Long,RestRelationship> lruRelCache = new LruCache<>("RestRelationship",10000);

    private final RestAPI restAPI;

    public RestEntityCache(RestAPI restAPI) {
        this.restAPI = restAPI;
    }

    public RestNode addToCache(RestNode node) {
        if (node == null) return null;
        long id = node.getId();
        if (id != -1) {
            RestNode existing = lruNodeCache.get(id);
            if (existing !=null) {
                if (existing != node) existing.updateFrom(node, restAPI);
                return existing;
            } else {
                lruNodeCache.put(id, node);
            }
        }
        return node;
    }
    public RestRelationship addToCache(RestRelationship rel) {
        if (rel == null) return null;
        long id = rel.getId();
        if (id != -1) {
            RestRelationship existing = lruRelCache.get(id);
            if (existing !=null) {
                if (existing != rel) existing.updateFrom(rel, restAPI);
                return existing;
            } else {
                lruRelCache.put(id, rel);
            }
        }
        return rel;
    }

    public RestNode getNode(long id) {
        return lruNodeCache.get(id);
    }
    public RestRelationship getRelationship(long id) {
        return lruRelCache.get(id);
    }

    public void removeNode(long id) {
        lruNodeCache.remove(id);
    }
    public void removeRelationship(long id) {
        lruRelCache.remove(id);
    }
}
