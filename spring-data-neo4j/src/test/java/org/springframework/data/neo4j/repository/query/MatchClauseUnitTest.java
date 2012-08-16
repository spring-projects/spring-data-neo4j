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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.neo4j.support.mapping.Neo4jMappingContext;

/**
 * Unit tests for {@link MatchClause}.
 * 
 * @author Oliver Gierke
 */
@Ignore("until merged with DATAGRAPH-113")
public class MatchClauseUnitTest {

    Neo4jMappingContext context;
    private VariableContext variableContext;

    @Before
    public void setUp() {
        context = new Neo4jMappingContext();
        context.setInitialEntitySet(Collections.singleton(Person.class));
        context.initialize();
        variableContext = new VariableContext();
    }

    @Test
    public void buildsMatchExpressionForSimpleTraversalCorrectly() {
        PropertyPath path = PropertyPath.from("group", Person.class);
        MatchClause clause = new MatchClause(context.getPersistentPropertyPath(path));
        assertThat(clause.toString(variableContext), is("person<-[:members]-person_group"));
    }

    @Test
    public void createsMatchClassForDeepTraversal() {

        PropertyPath path = PropertyPath.from("group.members.age", Person.class);
        MatchClause clause = new MatchClause(context.getPersistentPropertyPath(path));
        assertThat(clause.toString(variableContext), is("person<-[:members]-person_group-[:members]->person_group_members"));
    }

    @Test
    public void stopsAtNonRelationShipPropertyPath() {

        PropertyPath path = PropertyPath.from("group.name", Person.class);
        MatchClause clause = new MatchClause(context.getPersistentPropertyPath(path));
        assertThat(clause.toString(variableContext), is("person<-[:members]-person_group"));
    }
}
