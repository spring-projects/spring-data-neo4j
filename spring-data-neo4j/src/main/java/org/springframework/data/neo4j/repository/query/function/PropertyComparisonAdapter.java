package org.springframework.data.neo4j.repository.query.function;

import java.util.Map;

import org.neo4j.ogm.cypher.function.FilterFunction;
import org.neo4j.ogm.cypher.function.PropertyComparison;

/**
 * Adapter to the OGM FilterFunction interface for a PropertyComparison.
 *
 * @see FilterFunctionAdapter
 *
 * @author Jasper Blues
 */
public class PropertyComparisonAdapter implements FilterFunctionAdapter<Object> {

	private PropertyComparison propertyComparison;

	public PropertyComparisonAdapter(PropertyComparison propertyComparison) {
		this.propertyComparison = propertyComparison;
	}

	@Override
	public FilterFunction<Object> filterFunction() {
		return propertyComparison;
	}

	@Override
	public int parameterCount() {
		return 1;
	}

	@Override
	public void setValueFromArgs(Map<Integer, Object> params, int startIndex) {
		propertyComparison.setValue(params.get(startIndex));
	}
}
