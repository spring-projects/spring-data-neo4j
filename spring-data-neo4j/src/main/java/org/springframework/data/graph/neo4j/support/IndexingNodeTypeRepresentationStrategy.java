package org.springframework.data.graph.neo4j.support;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.FilteringIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.NodeTypeRepresentationStrategy;
import org.springframework.data.persistence.EntityInstantiator;

import java.util.HashMap;
import java.util.Map;

public class IndexingNodeTypeRepresentationStrategy implements NodeTypeRepresentationStrategy {

    public static final String INDEX_NAME = "__types__";
    public static final String TYPE_PROPERTY_NAME = "__type__";
    public static final String INDEX_KEY = "className";
    private EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;
    private GraphDatabaseService graphDb;
    private final Map<String,Class<?>> cache=new HashMap<String, Class<?>>();

    public IndexingNodeTypeRepresentationStrategy(GraphDatabaseService graphDb,
                                                  EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
		this.graphDb = graphDb;
		this.graphEntityInstantiator = graphEntityInstantiator;
    }

	private Index<Node> getNodeTypesIndex() {
		return graphDb.index().forNodes(INDEX_NAME);
	}

	private Index<Relationship> getRelTypesIndex() {
		return graphDb.index().forRelationships(INDEX_NAME);
	}

	@Override
	public void postEntityCreation(Node state, Class<? extends NodeBacked> type) {
        addToNodeTypesIndex(state, type);
        state.setProperty(TYPE_PROPERTY_NAME, type.getName());
	}

    private void addToNodeTypesIndex(Node node, Class<? extends NodeBacked> entityClass) {
		Class<?> klass = entityClass;
		while (klass.getAnnotation(NodeEntity.class) != null) {
			getNodeTypesIndex().add(node, INDEX_KEY, klass.getName());
			klass = klass.getSuperclass();
		}
	}

    @Override
    public <U extends NodeBacked> Iterable<U> findAll(Class<U> clazz) {
        return findAllNodeBacked(clazz);
    }

    private <ENTITY extends NodeBacked> Iterable<ENTITY> findAllNodeBacked(Class<ENTITY> clazz) {
		final IndexHits<Node> allEntitiesOfType = getNodeTypesIndex().get(INDEX_KEY, clazz.getName());
		return new FilteringIterable<ENTITY>(new IterableWrapper<ENTITY, Node>(allEntitiesOfType) {
			@Override
			@SuppressWarnings("unchecked")
			protected ENTITY underlyingObjectToObject(Node node) {
				Class<ENTITY> javaType = (Class<ENTITY>) getJavaType(node);
				if (javaType == null) return null;
				return graphEntityInstantiator.createEntityFromState(node, javaType);
			}
		}, new Predicate<ENTITY>() {
			@Override
			public boolean accept(ENTITY item) {
				return item != null;
			}
		});
	}

    @Override
    public long count(Class<? extends NodeBacked> entityClass) {
        long count = 0;
        for (Object o : getNodeTypesIndex().get(INDEX_KEY, entityClass.getName())) {
            count += 1;
        }
		return count;
	}


    @Override
    public Class<? extends NodeBacked> getJavaType(Node node) {
		if (node == null) throw new IllegalArgumentException("Node is null");
        String className = (String) node.getProperty(TYPE_PROPERTY_NAME);
        return getClassForName(className);
    }

    @SuppressWarnings({"unchecked"})
    private <ENTITY extends GraphBacked<?>> Class<ENTITY> getClassForName(String className) {
        try {
            Class<ENTITY> result= (Class<ENTITY>) cache.get(className);
            if (result!=null) return result;
            synchronized (cache) {
                result= (Class<ENTITY>) cache.get(className);
                if (result!=null) return result;
                result = (Class<ENTITY>) Class.forName(className);
                cache.put(className,result);
                return result;
            }
		} catch (NotFoundException e) {
			return null;
		} catch (ClassNotFoundException e) {
			return null;
		}
    }

    @Override
	public void preEntityRemoval(NodeBacked entity) {
        getNodeTypesIndex().remove(entity.getPersistentState());
	}

    @Override
    @SuppressWarnings("unchecked")
    public <U extends NodeBacked> U createEntity(Node state) {
        Class<? extends NodeBacked> javaType = getJavaType(state);
        if (javaType == null) {
            throw new IllegalStateException("No type stored on node.");
        }
        return (U) graphEntityInstantiator.createEntityFromState(state, javaType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U extends NodeBacked> U createEntity(Node state, Class<U> type) {
        Class<? extends NodeBacked> javaType = getJavaType(state);
        if (javaType == null) {
            throw new IllegalStateException("No type stored on node.");
        }
        if (type.isAssignableFrom(javaType)) {
            return (U) graphEntityInstantiator.createEntityFromState(state, javaType);
        }
        throw new IllegalArgumentException(String.format("Entity is not of type: %s (was %s)", type, javaType));
    }

    @Override
    public <U extends NodeBacked> U projectEntity(Node state, Class<U> type) {
        return graphEntityInstantiator.createEntityFromState(state, type);
    }
}
