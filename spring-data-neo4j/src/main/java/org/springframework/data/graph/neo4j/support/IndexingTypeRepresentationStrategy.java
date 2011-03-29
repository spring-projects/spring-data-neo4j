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
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.core.TypeRepresentationStrategy;
import org.springframework.data.persistence.EntityInstantiator;

import java.util.HashMap;
import java.util.Map;

public class IndexingTypeRepresentationStrategy implements TypeRepresentationStrategy {

    public static final String INDEX_NAME = "__types__";
    public static final String TYPE_PROPERTY_NAME = "__type__";
    public static final String INDEX_KEY = "className";
    private EntityInstantiator<NodeBacked, Node> graphEntityInstantiator;
    private EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator;
    private GraphDatabaseService graphDb;
    private final Map<String,Class<?>> cache=new HashMap<String, Class<?>>();

    public IndexingTypeRepresentationStrategy(GraphDatabaseService graphDb,
                                              EntityInstantiator<NodeBacked, Node> graphEntityInstantiator,
                                              EntityInstantiator<RelationshipBacked, Relationship> relationshipEntityInstantiator) {
		this.graphDb = graphDb;
		this.graphEntityInstantiator = graphEntityInstantiator;
        this.relationshipEntityInstantiator = relationshipEntityInstantiator;
    }

	private Index<Node> getNodeTypesIndex() {
		return graphDb.index().forNodes(INDEX_NAME);
	}

	private Index<Relationship> getRelTypesIndex() {
		return graphDb.index().forRelationships(INDEX_NAME);
	}

	@Override
	public void postEntityCreation(GraphBacked<?> entity) {
        if (entity instanceof NodeBacked) {
            NodeBacked nodeBacked = (NodeBacked) entity;
            Node node = nodeBacked.getPersistentState();
            Class<? extends NodeBacked> entityClass = nodeBacked.getClass();
            addToNodeTypesIndex(node, entityClass);
            node.setProperty(TYPE_PROPERTY_NAME, entityClass.getName());
        } else if (entity instanceof RelationshipBacked) {
            RelationshipBacked relationshipBacked = (RelationshipBacked) entity;
            Relationship rel = relationshipBacked.getPersistentState();
            Class<? extends RelationshipBacked> entityClass = relationshipBacked.getClass();
            addToRelTypesIndex(rel, entityClass);
            rel.setProperty(TYPE_PROPERTY_NAME, entityClass.getName());
        }
	}

    private void addToRelTypesIndex(Relationship rel, Class<? extends RelationshipBacked> entityClass) {
        getRelTypesIndex().add(rel, INDEX_KEY, entityClass.getName());
    }

    private void addToNodeTypesIndex(Node node, Class<? extends NodeBacked> entityClass) {
		Class<?> klass = entityClass;
		while (klass.getAnnotation(NodeEntity.class) != null) {
			getNodeTypesIndex().add(node, INDEX_KEY, klass.getName());
			klass = klass.getSuperclass();
		}
	}

	@Override
	public <ENTITY extends GraphBacked<?>> Iterable<ENTITY> findAll(Class<ENTITY> clazz) {
        if (NodeBacked.class.isAssignableFrom(clazz)) {
            return (Iterable<ENTITY>) findAllNodeBacked((Class<? extends NodeBacked>) clazz);
        } else if (RelationshipBacked.class.isAssignableFrom(clazz)) {
            return (Iterable<ENTITY>) findAllRelBacked((Class<? extends RelationshipBacked>) clazz);
        }
        throw new UnsupportedOperationException();
    }

    private <ENTITY extends RelationshipBacked> Iterable<ENTITY> findAllRelBacked(Class<ENTITY> clazz) {
        final IndexHits<Relationship> allEntitiesOfType = getRelTypesIndex().get(INDEX_KEY, clazz.getName());
        return new FilteringIterable<ENTITY>(new IterableWrapper<ENTITY, Relationship>(allEntitiesOfType) {
            @Override
            @SuppressWarnings("unchecked")
            protected ENTITY underlyingObjectToObject(Relationship rel) {
                Class<ENTITY> javaType = (Class<ENTITY>) getJavaType(rel);
                if (javaType == null) return null;
                return relationshipEntityInstantiator.createEntityFromState(rel, javaType);
            }
        }, new Predicate<ENTITY>() {
            @Override
            public boolean accept(ENTITY item) {
                return item != null;
            }
        });

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
	public long count(Class<? extends GraphBacked<?>> entityClass) {
        long count = 0;
        for (Object o : getIndexForType(entityClass).get(INDEX_KEY, entityClass.getName())) {
            count += 1;
        }
		return count;
	}

    private Index<?> getIndexForType(Class<? extends GraphBacked<?>> entityClass) {
        if (NodeBacked.class.isAssignableFrom(entityClass)) {
            return getNodeTypesIndex();
        } else if (RelationshipBacked.class.isAssignableFrom(entityClass)) {
            return getRelTypesIndex();
        }
        throw new UnsupportedOperationException();
    }

    @Override
	@SuppressWarnings("unchecked")
	public <ENTITY extends GraphBacked<?>> Class<ENTITY> getJavaType(PropertyContainer primitive) {
		if (primitive == null) throw new IllegalArgumentException("Node is null");
        String className = (String) primitive.getProperty(TYPE_PROPERTY_NAME);
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
	public void preEntityRemoval(GraphBacked<?> entity) {
        if (entity instanceof NodeBacked) {
		    getNodeTypesIndex().remove(((NodeBacked)entity).getPersistentState());
        } else if (entity instanceof RelationshipBacked) {
            getRelTypesIndex().remove(((RelationshipBacked)entity).getPersistentState());
        }
	}

	@Override
	public <T extends GraphBacked<?>> Class<T> confirmType(PropertyContainer primitive, Class<T> type) {
		Class<T> javaType = getJavaType(primitive);
		if (javaType == null) throw new IllegalStateException("No type stored on node.");
		if (type.isAssignableFrom(javaType)) return javaType;
		throw new IllegalArgumentException(String.format("%s does not correspond to the stored type %s of %s %s",
                type, javaType, primitive instanceof Node ? "node" : "relationship", primitive));
	}
}
