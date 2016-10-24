package org.springframework.data.neo4j.repository.query.function;

import java.util.Map;

import org.neo4j.ogm.cypher.function.FilterFunction;
import org.neo4j.ogm.cypher.function.PropertyComparison;
import org.springframework.data.neo4j.repository.query.derived.CypherFilter;

/**
 * Adapter to the OGM FilterFunction interface for a PropertyComparison.
 *
 * @author Jasper Blues
 * @see FilterFunctionAdapter
 */
public class PropertyComparisonAdapter implements FilterFunctionAdapter<Object> {

	private CypherFilter cypherFilter;
	private PropertyComparison propertyComparison;

	public PropertyComparisonAdapter(CypherFilter cypherFilter) {
		this.cypherFilter = cypherFilter;
		this.propertyComparison = new PropertyComparison();
	}

	public PropertyComparisonAdapter() {
		this(null);
	}

	public CypherFilter getCypherFilter() {
		return cypherFilter;
	}

	public void setCypherFilter(CypherFilter cypherFilter) {
		this.cypherFilter = cypherFilter;
	}

	@Override
	public CypherFilter cypherFilter() {
		return null;
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
	public void setValueFromArgs(Map<Integer, Object> params) {
		if (cypherFilter == null) {
			throw new IllegalStateException("Can't set value from args when cypherFilter is null.");
		}
		propertyComparison.setValue(params.get(cypherFilter.getPropertyPosition()));
	}
}
