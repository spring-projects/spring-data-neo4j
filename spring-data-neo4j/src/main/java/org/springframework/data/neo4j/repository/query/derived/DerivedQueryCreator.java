/*
 * Copyright 2011-2019 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      https://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
