/*
 * Copyright (c)  [2011-2019] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */
package org.springframework.data.neo4j.repository.query;

import java.util.Iterator;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.lang.Nullable;

/**
 * An {@link AbstractQueryCreator} that builds a query template based on filters. The intermediate object is a filter
 * definition whose state is modified during query creation.
 *
 * @author Luanne Misquitta
 * @author Michael J. Simons
 */
class TemplatedQueryCreator extends AbstractQueryCreator<TemplatedQuery, FilterBuildersDefinition> {

	private final Class<?> entityType;

	public TemplatedQueryCreator(PartTree tree, Class<?> entityType) {
		super(tree);

		this.entityType = entityType;
	}

	@Override
	protected FilterBuildersDefinition create(Part part, Iterator<Object> iterator) {
		return FilterBuildersDefinition.forType(entityType) //
				.startWith(part);
	}

	@Override
	protected FilterBuildersDefinition and(Part part, FilterBuildersDefinition base, Iterator<Object> iterator) {
		return base.and(part);
	}

	@Override
	protected FilterBuildersDefinition or(FilterBuildersDefinition base, FilterBuildersDefinition criteria) {
		return base.or(criteria.getBasePart());
	}

	@Override
	protected TemplatedQuery complete(@Nullable FilterBuildersDefinition filterDefinition, Sort sort) {

		return Optional.ofNullable(filterDefinition)
				.map(FilterBuildersDefinition::buildTemplatedQuery)
				.orElseGet(TemplatedQuery::unfiltered);
	}
}
