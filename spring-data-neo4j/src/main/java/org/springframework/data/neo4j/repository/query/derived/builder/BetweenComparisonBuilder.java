package org.springframework.data.neo4j.repository.query.derived.builder;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.neo4j.ogm.cypher.function.PropertyComparison;
import org.springframework.data.repository.query.parser.Part;

/**
 * @author Jasper Blues
 * @author Nicolas Mervaillie
 */
public class BetweenComparisonBuilder extends FilterBuilder {

	public BetweenComparisonBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		super(part, booleanOperator, entityType);
	}

	@Override
	public List<Filter> build(Stack<Object> params) {
		Filter gt = new Filter();
		gt.setPropertyName(propertyName());
		gt.setOwnerEntityType(entityType);
		gt.setBooleanOperator(booleanOperator);
		gt.setNegated(isNegated());
		gt.setComparisonOperator(ComparisonOperator.GREATER_THAN);
		gt.setFunction(new PropertyComparison(params.pop()));
		setNestedAttributes(part, gt);

		Filter lt = new Filter();
		lt.setPropertyName(propertyName());
		lt.setOwnerEntityType(entityType);
		lt.setBooleanOperator(BooleanOperator.AND);
		lt.setNegated(isNegated());
		lt.setComparisonOperator(ComparisonOperator.LESS_THAN);
		lt.setFunction(new PropertyComparison(params.pop()));
		setNestedAttributes(part, lt);

		return Arrays.asList(gt, lt);
	}
}
