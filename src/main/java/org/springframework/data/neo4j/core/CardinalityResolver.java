import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CardinalityResolver {
	Object resolveRelation(List<?> newRelatedObjects, Map<?, ?> newRelatedObjectsByType);
}
public class OneToOneResolver implements CardinalityResolver {
	@Override
	public Object resolveRelation(List<?> newRelatedObjects, Map<?, ?> newRelatedObjectsByType) {
		return Optional.ofNullable(newRelatedObjects).flatMap(v -> v.stream().findFirst()).orElse(null);
	}
}

public class OneToManyResolver implements CardinalityResolver {
	@Override
	public Object resolveRelation(List<?> newRelatedObjects, Map<?, ?> newRelatedObjectsByType) {
		return newRelatedObjects.isEmpty() ? null : newRelatedObjects;
	}
}

public class DynamicCardinalityResolver implements CardinalityResolver {
	@Override
	public Object resolveRelation(List<?> newRelatedObjects, Map<?, ?> newRelatedObjectsByType) {
		return newRelatedObjectsByType.isEmpty() ? null : newRelatedObjectsByType;
	}
}