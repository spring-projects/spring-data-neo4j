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

package org.springframework.data.neo4j.repository;

import org.neo4j.helpers.collection.MapUtil;
import org.springframework.data.neo4j.model.*;
import org.springframework.data.neo4j.template.Neo4jOperations;

import java.util.Arrays;
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
    public Friendship friendShip;

    public TestTeam() {
    }

    public TestTeam createSDGTeam(GraphRepository<Person> repo, GraphRepository<Group> groupRepo, GraphRepository<Friendship> friendshipRepository) {
        emil = new Person("Emil", 30);
        emil.setAlias("emilAlias");

        michael = new Person("Michael", 36);
        michael.setBoss(emil);
        michael.setPersonality(Personality.EXTROVERT);
        michael.setLocation( 16, 56 );
        michael.setAlias("michaelAlias");

        david = new Person("David", 25);
        david.setAlias("davidAlias");
        david.setBoss(emil);
        david.setLocation( 16.5, 56.5 );
        friendShip = michael.knows(david);
        friendShip.setYears(2);
        sdg = new Group();
        sdg.setName("SDG");
        sdg.addPerson(michael);
        sdg.addPerson(emil);
        sdg.addPerson(david);

        repo.save(Arrays.asList(emil, david, michael));
        friendshipRepository.save(friendShip);
        groupRepo.save(sdg);
        return this;
    }

    public Map<String, Object> simpleRowFor(final Person person, String prefix) {
        return MapUtil.map(prefix+".name", person.getName(), prefix+".age", person.getAge());
    }

    public TestTeam createSDGTeam(Neo4jOperations template) {
        return createSDGTeam(template.repositoryFor(Person.class), template.repositoryFor(Group.class), template.repositoryFor(Friendship.class));
    }
}
