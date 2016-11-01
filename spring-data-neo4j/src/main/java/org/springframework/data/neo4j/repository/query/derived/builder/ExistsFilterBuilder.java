package org.springframework.data.neo4j.repository.query.derived.builder;

import static org.springframework.data.repository.query.parser.Part.Type.IS_NOT_NULL;

import java.util.ArrayList;
import java.util.List;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.neo4j.ogm.cypher.ComparisonOperator;
import org.springframework.data.neo4j.repository.query.derived.CypherFilter;
import org.springframework.data.repository.query.parser.Part;

/**
 * @author Jasper Blues
 */
public class ExistsFilterBuilder extends CypherFilterBuilder {

	public ExistsFilterBuilder(Part part, BooleanOperator booleanOperator, Class<?> entityType) {
		super(part, booleanOperator, entityType);
	}

	@Override
	public List<CypherFilter> build() {
		List<CypherFilter> filters = new ArrayList<>();

		CypherFilter filter = new CypherFilter();
		filter.setPropertyName(propertyName());
		filter.setOwnerEntityType(entityType);
		filter.setBooleanOperator(booleanOperator);
		filter.setNegated(isNegated());
		filter.setComparisonOperator(ComparisonOperator.EXISTS);
		setNestedAttributes(part, filter);

		filters.add(filter);

		return filters;
	}

}
