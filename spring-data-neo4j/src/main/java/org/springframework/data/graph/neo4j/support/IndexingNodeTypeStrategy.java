package org.springframework.data.graph.neo4j.support;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.NotFoundException;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;
import org.neo4j.helpers.Predicate;
import org.neo4j.helpers.collection.FilteringIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.NodeTypeStrategy;
import org.springframework.data.persistence.EntityInstantiator;

import java.util.HashMap;
import java.util.Map;

public class IndexingNodeTypeStrategy implements NodeTypeStrategy {

    public static final String NODE_INDEX_NAME = "__types__";
    public static final String TYPE_PROPERTY_NAME = "__type__";
    public static final String INDEX_KEY = "className";
    private EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;
	private GraphDatabaseService graphDb;
    private final Map<String,Class<?>> cache=new HashMap<String, Class<?>>();

    public IndexingNodeTypeStrategy(GraphDatabaseService graphDb, EntityInstantiator<NodeBacked, Node> graphEntityInstantiator) {
		this.graphDb = graphDb;
		this.graphEntityInstantiator = graphEntityInstantiator;
	}

	private Index<Node> getTypesIndex() {
		return graphDb.index().forNodes(NODE_INDEX_NAME);
	}

	@Override
	public void postEntityCreation(NodeBacked entity) {
		Node node = entity.getPersistentState();
		Class<? extends NodeBacked> entityClass = entity.getClass();
		addToTypesIndex(node, entityClass);
		node.setProperty(TYPE_PROPERTY_NAME, entityClass.getName());
	}

	private void addToTypesIndex(Node node, Class<? extends NodeBacked> entityClass) {
		Class<?> klass = entityClass;
		while (klass.getAnnotation(NodeEntity.class) != null) {
			getTypesIndex().add(node, INDEX_KEY, klass.getName());
			klass = klass.getSuperclass();
		}
	}

	@Override
	public <ENTITY extends NodeBacked> Iterable<ENTITY> findAll(Class<ENTITY> clazz) {
		final IndexHits<Node> allEntitiesOfType = getTypesIndex().get(INDEX_KEY, clazz.getName());
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
        for (Node node : getTypesIndex().get(INDEX_KEY, entityClass.getName())) {
            count += 1;
        }
		return count;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <ENTITY extends NodeBacked> Class<ENTITY> getJavaType(Node node) {
		if (node == null) throw new IllegalArgumentException("Node is null");
        String className = (String) node.getProperty(TYPE_PROPERTY_NAME);
        return getClassForName(className);
    }

    @SuppressWarnings({"unchecked"})
    private <ENTITY extends NodeBacked> Class<ENTITY> getClassForName(String className) {
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
		getTypesIndex().remove(entity.getPersistentState());
	}

	@Override
	public <T extends NodeBacked> Class<T> confirmType(Node node, Class<T> type) {
		Class<T> javaType = getJavaType(node);
		if (javaType == null) throw new IllegalStateException("No type stored on node.");
		if (type.isAssignableFrom(javaType)) return javaType;
		throw new IllegalArgumentException(String.format("%s does not correspond to the node type %s of node %s", type, javaType, node));
	}
}
