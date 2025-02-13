/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.repository.query;

import org.neo4j.cypherdsl.core.Condition;
import org.neo4j.cypherdsl.core.Expression;
import org.neo4j.cypherdsl.core.PatternElement;
import org.neo4j.cypherdsl.core.SortItem;
import org.neo4j.cypherdsl.core.Statement;
import org.springframework.data.neo4j.core.mapping.Neo4jPersistentEntity;
import org.springframework.data.neo4j.core.mapping.PropertyFilter;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author Gerrit Meier
 */
public interface CustomStatementCreator {
	Statement createStatement(List<PatternElement> matchOn, Condition condition, Predicate<PropertyFilter.RelaxedPropertyPath> includeField, Neo4jPersistentEntity<?> returnTuple, Collection<Expression> returnExpressions, Collection<SortItem> orderBy, Long skip, Number limit);
}
