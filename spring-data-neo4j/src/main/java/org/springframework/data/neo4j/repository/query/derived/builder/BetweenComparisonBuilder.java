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
		final Object value1 = params.pop();
		Filter gt = new Filter(propertyName(), ComparisonOperator.GREATER_THAN, value1);
		gt.setOwnerEntityType(entityType);
		gt.setBooleanOperator(booleanOperator);
		gt.setNegated(isNegated());
		gt.setFunction(new PropertyComparison(value1));
		setNestedAttributes(part, gt);

		final Object value2 = params.pop();
		Filter lt = new Filter(propertyName(), ComparisonOperator.LESS_THAN, value2);
		lt.setOwnerEntityType(entityType);
		lt.setBooleanOperator(BooleanOperator.AND);
		lt.setNegated(isNegated());
		lt.setFunction(new PropertyComparison(value2));
		setNestedAttributes(part, lt);

		return Arrays.asList(gt, lt);
	}
}
