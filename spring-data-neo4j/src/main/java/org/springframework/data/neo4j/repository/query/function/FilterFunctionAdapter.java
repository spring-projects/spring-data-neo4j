package org.springframework.data.neo4j.repository.query.function;

import java.util.Map;

import org.neo4j.ogm.cypher.function.FilterFunction;
import org.springframework.data.neo4j.repository.query.derived.CypherFilter;

/**
 * Adapter to the OGM FilterFunction interface. Adds the derived finder parameter count, and the ability to set the
 * function value from the derived finder argument structure.
 *
 * @author Jasper Blues
 */
public interface FilterFunctionAdapter<T> {

	CypherFilter cypherFilter();

	FilterFunction<T> filterFunction();

	int parameterCount();

	void setValueFromArgs(Map<Integer, Object> params);

}
