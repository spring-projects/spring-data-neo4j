package org.springframework.data.neo4j.core;

import java.util.List;
import java.util.Map;

public class DynamicCardinalityResolver implements CardinalityResolver {
	@Override
	public Object resolveRelation(List<?> newRelatedObjects, Map<?, ?> newRelatedObjectsByType) {
		return newRelatedObjectsByType.isEmpty() ? null : newRelatedObjectsByType;
	}
}
