package org.springframework.data.neo4j.repository.query.function;

import java.util.Map;

import org.neo4j.ogm.cypher.function.DistanceComparison;
import org.neo4j.ogm.cypher.function.DistanceFromPoint;
import org.neo4j.ogm.cypher.function.FilterFunction;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.neo4j.repository.query.derived.CypherFilter;

/**
 * Adapter to the OGM FilterFunction interface for a PropertyComparison.
 *
 * @author Jasper Blues
 */
public class DistanceComparisonAdapter implements FilterFunctionAdapter<DistanceFromPoint> {

	private CypherFilter cypherFilter;
	private DistanceComparison distanceComparison;

	public DistanceComparisonAdapter(CypherFilter cypherFilter) {
		this.distanceComparison = new DistanceComparison();
		this.cypherFilter = cypherFilter;
	}

	public DistanceComparisonAdapter() {
		this(null);
	}

	@Override
	public CypherFilter cypherFilter() {
		return cypherFilter;
	}

	@Override
	public FilterFunction<DistanceFromPoint> filterFunction() {
		return distanceComparison;
	}

	@Override
	public int parameterCount() {
		return 2;
	}

	@Override
	public void setValueFromArgs(Map<Integer, Object> params) {
		if (cypherFilter == null) {
			throw new IllegalStateException("Can't set value from args when cypherFilter is null.");
		}

		Object firstArg = params.get(cypherFilter().getPropertyPosition());
		Object secondArg = params.get(cypherFilter().getPropertyPosition() + 1);

		Distance distance;
		Point point;

		if (firstArg instanceof Distance && secondArg instanceof Point) {
			distance = (Distance) firstArg;
			point = (Point) secondArg;
		} else if (secondArg instanceof Distance && firstArg instanceof Point) {
			distance = (Distance) secondArg;
			point = (Point) firstArg;
		} else {
			throw new IllegalArgumentException("findNear requires an argument of type Distance and an argument of type Point");
		}

		double meters;
		if (distance.getMetric() == Metrics.KILOMETERS) {
			meters = distance.getValue() * 1000.0d;
		} else if (distance.getMetric() == Metrics.MILES) {
			meters = distance.getValue() / 0.00062137d;
		} else {
			meters = distance.getValue();
		}

		distanceComparison.setValue(new DistanceFromPoint(point.getX(), point.getY(), distance.getValue() * meters));
	}
}
