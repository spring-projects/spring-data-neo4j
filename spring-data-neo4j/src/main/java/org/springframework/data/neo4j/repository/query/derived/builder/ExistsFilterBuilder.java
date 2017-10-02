package org.springframework.data.neo4j.repository.query.derived.builder;

import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.neo4j.ogm.cypher.Filter;
import org.springframework.data.repository.query.parser.Part;

/**
 * @author Jasper Blues
 * @author Nicolas Mervaillie
 */
public class ExistsFilterBuilder extends FilterBuilder {

	public ExistsFilterBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		super(part, booleanOperator, entityType);
	}

	@Override
	public List<Filter> build(Stack<Object> params) {
		Filter filter = new Filter(propertyName(), ComparisonOperator.EXISTS);
		filter.setOwnerEntityType(entityType);
		filter.setBooleanOperator(booleanOperator);
		filter.setNegated(isNegated());
		setNestedAttributes(part, filter);

		return Collections.singletonList(filter);
	}

}
