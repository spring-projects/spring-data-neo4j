package org.springframework.data.neo4j.core;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CardinalityResolver {
	Object resolveRelation(List<?> newRelatedObjects, Map<?, ?> newRelatedObjectsByType);
}

