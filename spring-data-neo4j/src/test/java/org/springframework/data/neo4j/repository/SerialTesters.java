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
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;
import org.springframework.data.neo4j.model.Friendship;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.model.Personality;
import org.springframework.data.neo4j.template.Neo4jOperations;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * Creates a whole bunch of SDN entities for testing various aspects of the
 * Serialization process.
 *
 * @author Nicki Watt
 * @since 06.08.2013
 */
public class SerialTesters {

    public SimpleDateFormat bdayFormatter;

    public Person michael;
    public Person emil;
    public Person david;
    public Person tareq;
    public Person nicki;
    public Group serialTesterGroup;
    public Friendship friendShip;
    public Friendship friendShip2;
    public Friendship friendShip3;

    public SerialTesters createUpgraderTeam(GraphRepository<Person> repo, GraphRepository<Group> groupRepo, GraphRepository<Friendship> friendshipRepository)
    {
        emil = new Person("Emil", 30);

        michael = new Person("Michael", 36);
        michael.setBoss(emil);
        michael.setPersonality(Personality.EXTROVERT);
        michael.setLocation( "POINT(16 56)" );

        david = new Person("David", 25);
        david.setBoss(emil);
        david.setLocation( 16.5, 56.5 );

        tareq = new Person("Tareq", 36);
        nicki = new Person("Nicki", 36);
        bdayFormatter = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");
        try {
            nicki.setBirthdate(bdayFormatter.parse("01 JAN 2013 00:00:00"));
        } catch (ParseException e) {
            throw new RuntimeException("Could not parse date ...");
        }
        nicki.setBoss(tareq);
        nicki.setDynamicProperty("What is this???");
        nicki.setHeight((short)100);
        nicki.setLocation(51.5, 0.1);
        nicki.setPersonalProperties(new DynamicPropertiesContainer());
        nicki.setProperty("addressLine1","Somewhere");
        nicki.setProperty("addressLine2","Over the rainbow");
        nicki.setNickname("Nicks");
        friendShip2 = nicki.knows(tareq);
        friendShip2.setYears(2);
        friendShip3 = nicki.knows(michael);
        friendShip3.setYears(0);

        friendShip = michael.knows(david);
        friendShip.setYears(2);
        serialTesterGroup = new Group();
        serialTesterGroup.setName("SDN-upgraders");
        serialTesterGroup.addPerson(michael);
        serialTesterGroup.addPerson(nicki);
        serialTesterGroup.addPerson(david);

        repo.save(asList(emil, david, michael, nicki, tareq));
        friendshipRepository.save(friendShip);
        friendshipRepository.save(friendShip2);
        friendshipRepository.save(friendShip3);

        groupRepo.save(serialTesterGroup);
        return this;
    }

}
