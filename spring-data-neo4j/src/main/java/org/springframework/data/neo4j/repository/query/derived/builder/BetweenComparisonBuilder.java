package org.springframework.data.neo4j.repository.query.derived.builder;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.springframework.data.neo4j.repository.query.derived.CypherFilter;
import org.springframework.data.repository.query.parser.Part;

/**
 * @author Jasper Blues
 */
public class BetweenComparisonBuilder extends CypherFilterBuilder {

	public BetweenComparisonBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		super(part, booleanOperator, entityType);
	}

	@Override
	public List<CypherFilter> build() {
		List<CypherFilter> filters = new ArrayList<>();

		CypherFilter greaterThan = new CypherFilter();
		greaterThan.setPropertyName(propertyName());
		greaterThan.setOwnerEntityType(entityType);
		greaterThan.setBooleanOperator(booleanOperator);
		greaterThan.setNegated(isNegated());
		greaterThan.setComparisonOperator(ComparisonOperator.GREATER_THAN);
		setNestedAttributes(part, greaterThan);
		filters.add(greaterThan);

		CypherFilter lessThan = new CypherFilter();
		lessThan.setPropertyName(propertyName());
		lessThan.setOwnerEntityType(entityType);
		lessThan.setBooleanOperator(BooleanOperator.AND);
		lessThan.setNegated(isNegated());
		lessThan.setComparisonOperator(ComparisonOperator.LESS_THAN);
		setNestedAttributes(part, lessThan);
		filters.add(lessThan);

		return filters;
	}
}
