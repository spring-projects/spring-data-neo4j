/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

package org.springframework.data.neo4j.repository.query.derived;

import java.util.Iterator;

import org.neo4j.ogm.cypher.BooleanOperator;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * An {@link AbstractQueryCreator} that builds a graph query.
 *
 * @author Luanne Misquitta
 */
public class DerivedQueryCreator extends AbstractQueryCreator<DerivedQueryDefinition, DerivedQueryBuilder> {

	private final Class<?> entityType;

	public DerivedQueryCreator(PartTree tree, Class<?> entityType) {
		super(tree);
		this.entityType = entityType;
	}

	@Override
	protected DerivedQueryBuilder create(Part part, Iterator<Object> iterator) {
		DerivedQueryBuilder queryBuilder = new DerivedQueryBuilder(entityType, part);
		queryBuilder.addPart(part, BooleanOperator.NONE);
		return queryBuilder;
	}

	@Override
	protected DerivedQueryBuilder and(Part part, DerivedQueryBuilder base, Iterator<Object> iterator) {
		base.addPart(part, BooleanOperator.AND);
		return base;
	}

	@Override
	protected DerivedQueryBuilder or(DerivedQueryBuilder base, DerivedQueryBuilder criteria) {
		base.addPart(criteria, BooleanOperator.OR);
		return base;
	}

	@Override
	protected DerivedQueryDefinition complete(DerivedQueryBuilder builder, Sort sort) {
		return builder.buildQuery();
	}
}
