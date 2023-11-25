package org.springframework.data.neo4j.core;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OneToOneResolver implements CardinalityResolver {
	@Override
	public Object resolveRelation(List<?> newRelatedObjects, Map<?, ?> newRelatedObjectsByType) {
		return Optional.ofNullable(newRelatedObjects).flatMap(v -> v.stream().findFirst()).orElse(null);
	}
}
