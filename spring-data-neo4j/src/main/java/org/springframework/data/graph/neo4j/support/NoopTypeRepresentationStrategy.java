package org.springframework.data.graph.neo4j.support;

import org.neo4j.graphdb.Node;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.TypeRepresentationStrategy;

public class NoopTypeRepresentationStrategy implements TypeRepresentationStrategy {
	@Override
	public void postEntityCreation(NodeBacked entity) {
	}

	@Override
	public <T extends NodeBacked> Iterable<T> findAll(Class<T> clazz) {
		throw new UnsupportedOperationException("findAll not supported by NoopTypeRepresentationStrategy.");
	}

	@Override
	public long count(Class<? extends NodeBacked> entityClass) {
		throw new UnsupportedOperationException("count not supported by NoopTypeRepresentationStrategy.");
	}

	@Override
	public <T extends NodeBacked> Class<T> getJavaType(Node node) {
		throw new UnsupportedOperationException("getJavaType not supported NoopTypeRepresentationStrategy.");
	}

	@Override
	public void preEntityRemoval(NodeBacked entity) {
	}

	@Override
	public <T extends NodeBacked> Class<T> confirmType(Node node, Class<T> type) {
		return type;
	}
}
