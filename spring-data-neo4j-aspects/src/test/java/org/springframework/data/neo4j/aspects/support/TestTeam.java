/**
 * Copyright 2011 the original author or authors.
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

package org.springframework.data.neo4j.aspects.support;

import org.neo4j.helpers.collection.MapUtil;
import org.springframework.data.neo4j.aspects.Group;
import org.springframework.data.neo4j.aspects.Person;
import org.springframework.data.neo4j.aspects.Personality;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import java.util.Map;

/**
 * @author mh
 * @since 13.06.11
 */
public class TestTeam {
    public Person michael;
    public Person emil;
    public Person david;
    public Group sdg;
    private final Neo4jTemplate template;

    public TestTeam(Neo4jTemplate template) {
        this.template = template;
    }

    public void createSDGTeam() {
        michael = Person.persistedPerson("Michael", 36);
        emil = Person.persistedPerson("Emil", 30);
        michael.setBoss(emil);
        michael.setPersonality(Personality.EXTROVERT);
        david = Person.persistedPerson("David", 25);
        david.setBoss(emil);
        sdg = template.save(new Group());
        sdg.setName("SDG");
        sdg.addPerson(michael);
        sdg.addPerson(emil);
        sdg.addPerson(david);
        // todo those should be attached and automatically written through to the db
        template.save(david);
        template.save(emil);
        template.save(michael);
        template.save(sdg);
    }

    public Map<String, Object> simpleRowFor(final Person person, String prefix) {
        return MapUtil.map(prefix+".name", person.getName(), prefix+".age", person.getAge());
    }
}
