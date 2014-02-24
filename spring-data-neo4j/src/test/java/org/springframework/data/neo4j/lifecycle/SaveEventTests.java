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
package org.springframework.data.neo4j.lifecycle;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestGraphDatabaseFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.annotation.relatedto.Experience;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.node.Neo4jHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.BeforeTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.neo4j.SetHelper.asSet;

@RelationshipEntity(type = "slew")
class Slaying {
    @GraphId
    Long id;

    @StartNode
    FictionalCharacter slayor;

    @EndNode
    FictionalCharacter slayee;

    String battle;

    Slaying() {
    }

    Slaying(FictionalCharacter slayor, FictionalCharacter slayee, String battle) {
        this.slayor = slayor;
        this.slayee = slayee;
        this.battle = battle;
    }
}


@NodeEntity
class FictionalCharacter {
    @GraphId
    Long id;

    String name;

    @RelatedToVia
    Slaying slew;

    @RelatedToVia(type = "slayered")
    Set<Slaying> slayings;

    FictionalCharacter() {
    }

    FictionalCharacter(String name) {

        this.name = name;
    }

    void slew(FictionalCharacter fictionalCharacter, String battle) {
        slew = new Slaying(this, fictionalCharacter, battle);
    }

    void slew(FictionalCharacter... victims) {
        slayings = new HashSet<Slaying>();

        for (FictionalCharacter victim : victims) {
            slayings.add(new Slaying(this, victim, null));
        }
    }
}

@NodeEntity
class Parent {
    @GraphId
    Long id;

    String name;

    @Fetch
    Child eldest;

    @Fetch
    Set<Child> youngsters;

    Parent() {

    }

    Parent(Child eldest) {
        this.eldest = eldest;
    }

    public Parent(String name) {
        this.name = name;
    }

    public Parent(Set<Child> youngsters) {
        this.youngsters = youngsters;
    }
}

@NodeEntity
class Child {
    @GraphId
    Long id;

    String name;

    Child() {
    }

    Child(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Child child = (Child) o;

        if (id != null ? !id.equals(child.id) : child.id != null) return false;
        if (name != null ? !name.equals(child.name) : child.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@Transactional
public class SaveEventTests {
    @Configuration
    @EnableNeo4jRepositories
    static class TestConfig extends Neo4jConfiguration {

        public TestConfig() throws ClassNotFoundException {
            setBasePackage(SaveEventTests.class.getPackage().getName());
        }

        @Bean
        GraphDatabaseService graphDatabaseService() {
            return new TestGraphDatabaseFactory().newImpermanentDatabase();
        }

        @Bean
        ApplicationListener<BeforeSaveEvent> beforeSaveEventApplicationListener() {
            return new ApplicationListener<BeforeSaveEvent>() {
                public void onApplicationEvent(BeforeSaveEvent event) {
                    entities.add(event.getEntity());
                }
            };
        }
    }

    @Autowired
    Neo4jTemplate template;

    static final LinkedList<Object> entities = new LinkedList<Object>();

    @BeforeTransaction
    public void beforeTransaction() {
        Neo4jHelper.cleanDb(template);
    }

    @Before
    public void before() {
        entities.clear();
    }

    @Test
    public void shouldFireEventForNodeEntity() throws Exception {
        Child child = new Child();
        template.save(child);

        assertThat(entities.size(), is(1));
        assertThat((Child) entities.get(0), is(child));
    }

    @Test
    public void shouldFireEventForRelationshipEntity() throws Exception {
        FictionalCharacter fingolfin = template.save(new FictionalCharacter("Fingolfin"));
        FictionalCharacter morgoth = template.save(new FictionalCharacter("Morgoth"));
        entities.clear();
        Slaying slaying = new Slaying(morgoth, fingolfin, "Dagor Bragollach");

        template.save(slaying);

        assertThat(entities.size(), is(1));
        assertThat(((Slaying) entities.get(0)).battle, is(equalTo("Dagor Bragollach")));
    }

    @Test
    public void shouldFireEventForNodeEntities() throws Exception {
        Child child1 = new Child("Huey");
        Child child2 = new Child("Louie");
        Child child3 = new Child("Dewey");
        Parent parent = new Parent(asSet(child1, child2, child3));
        template.save(parent);

        assertThat(entities.size(), is(4));
        assertThat((Parent) entities.get(0), is(parent));
        assertThat((Child) entities.get(1), is(child1));
        assertThat((Child) entities.get(2), is(child2));
        assertThat((Child) entities.get(3), is(child3));
    }

    @Test
    public void shouldFireEventForRelationshipEntities() throws Exception {
        FictionalCharacter beleg = template.save(new FictionalCharacter("Beleg"));
        FictionalCharacter brandir = template.save(new FictionalCharacter("Brandir"));
        entities.clear();
        FictionalCharacter turin = new FictionalCharacter("Túrin");
        turin.slew(beleg, brandir);

        template.save(turin);

        assertThat(entities.size(), is(3));
        assertThat(((FictionalCharacter) entities.get(0)).id, is(turin.id));
        assertThat(((Slaying) entities.get(1)).slayee.id, is(Matchers.<Long>either(equalTo(beleg.id)).or(equalTo(brandir.id))));
        assertThat(((Slaying) entities.get(2)).slayee.id, is(Matchers.<Long>either(equalTo(beleg.id)).or(equalTo(brandir.id))));
    }

    @Test
    public void shouldFireEventForNodeEntityWhenItIsSavedIndirectly() throws Exception {
        Child child = new Child();
        Parent parent = new Parent(child);
        template.save(parent);

        assertThat(entities.size(), is(2));
        assertThat((Parent) entities.get(0), is(parent));
        assertThat((Child) entities.get(1), is(child));
    }

    @Test
    public void shouldFireEventForRelationshipEntityWhenItIsSavedIndirectly() throws Exception {
        FictionalCharacter fingolfin = template.save(new FictionalCharacter("Fingolfin"));
        entities.clear();
        FictionalCharacter morgoth = new FictionalCharacter("Morgoth");
        morgoth.slew(fingolfin, "Dagor Bragollach");

        template.save(morgoth);

        assertThat(entities.size(), is(2));
        assertThat(((FictionalCharacter) entities.get(0)).id, is(morgoth.id));
        assertThat(((Slaying) entities.get(1)).battle, is(equalTo("Dagor Bragollach")));
    }

    @Test
    public void shouldFireEventForUpdatedEntities() throws Exception {
        Parent parent = template.save(new Parent("Donald Duck"));
        entities.clear();
        parent = template.findOne(parent.id, Parent.class);
        parent.name = "Mickey Mouse";

        template.save(parent);

        assertThat(entities.size(), is(1));
        assertThat((Parent) entities.get(0), is(parent));
    }

    @Test
    public void shouldFireEventEvenIfEntityHasNotBeenUpdated() throws Exception {
        Parent parent = template.save(new Parent("Uncle Scrooge"));
        entities.clear();
        parent = template.findOne(parent.id, Parent.class);

        template.save(parent);

        assertThat(entities.size(), is(1));
        assertThat((Parent) entities.get(0), is(parent));
    }

    /**
     * because we do not save recursively, the child entity isn't saved and thus no event is fired
     * <p/>
     * so this is basically saying, "we do not save recursively"
     * <p/>
     * is that different in the advanced mapping mode case?
     */
    @Test
    public void shouldNotFireEventForUpdatedRelatedEntities() throws Exception {
        Parent parent = template.save(new Parent(new Child("Daisy Duck")));
        entities.clear();
        parent = template.findOne(parent.id, Parent.class);
        parent.eldest.name = "Minnie Mouse";

        template.save(parent);

        assertThat(entities.size(), is(1));
        assertThat((Parent) entities.get(0), is(parent));
    }
}
