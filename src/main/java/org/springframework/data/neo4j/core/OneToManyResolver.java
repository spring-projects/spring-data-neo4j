package org.springframework.data.neo4j.core;

import java.util.List;
import java.util.Map;

public class OneToManyResolver implements CardinalityResolver {
	@Override
	public Object resolveRelation(List<?> newRelatedObjects, Map<?, ?> newRelatedObjectsByType) {
		return newRelatedObjects.isEmpty() ? null : newRelatedObjects;
	}
}
