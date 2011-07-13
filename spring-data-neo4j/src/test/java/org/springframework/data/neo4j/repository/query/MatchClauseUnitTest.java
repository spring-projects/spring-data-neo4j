/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.neo4j.repository.query;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.core.Direction;
import org.springframework.data.neo4j.mapping.Neo4JMappingContext;
import org.springframework.data.repository.query.parser.Property;

/**
 * 
 * @author Oliver Gierke
 */
public class MatchClauseUnitTest {

	Neo4JMappingContext context;

	@Before
	public void setUp() {
		context = new Neo4JMappingContext();
		context.setInitialEntitySet(Collections.singleton(Person.class));
		context.afterPropertiesSet();
	}

	@Test
	public void buildsMatchExpressionForSimpleTraversalCorrectly() {

		MatchClause clause = new MatchClause(context, Property.from("group", Person.class));
		assertThat(clause.toString(), is("(person)<-[:members]-(group)"));
	}

	@Test
	public void createsMatchClassForDeepTraversal() {

		MatchClause clause = new MatchClause(context, Property.from("group.members.age", Person.class));
		assertThat(clause.toString(), is("(person)<-[:members]-(group)-[:members]->(members)"));
	}

	@Test
	public void stopsAtNonRelationShipProperty() {

		MatchClause clause = new MatchClause(context, Property.from("group.name", Person.class));
		assertThat(clause.toString(), is("(person)<-[:members]-(group)"));
	}

	@NodeEntity
	class Person {

		private int age;

		@RelatedTo(type = "members", direction = Direction.INCOMING)
		private Group group;
	}

	class Group {

		@Indexed
		private String name;

		@RelatedTo(type = "members", direction = Direction.OUTGOING)
		private Set<Person> members;
	}
}
