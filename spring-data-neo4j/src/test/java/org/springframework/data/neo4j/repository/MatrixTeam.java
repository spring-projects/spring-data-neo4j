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

import org.springframework.data.neo4j.model.Friendship;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.model.Person;
import org.springframework.data.neo4j.model.Personality;
import org.springframework.data.neo4j.template.Neo4jOperations;

import java.util.Arrays;

/**
 * @author Nicki Watt
 * @since 02.09.2013
 */
public class MatrixTeam {
    public Person neo;
    public Person cypher;
    public Person trinity;
    public Group matrixGroup;

    public MatrixTeam() {
    }

    public MatrixTeam createMatrixTeam(GraphRepository<Person> repo, GraphRepository<Group> groupRepo, GraphRepository<Friendship> friendshipRepository) {
        cypher = new Person("Cypher", 30);
        neo = new Person("Neo", 36);
        neo.setPersonality(Personality.EXTROVERT);
        neo.setLocation(16, 56);

        trinity = new Person("Trinity", 25);
        trinity.setBoss(cypher);
        trinity.setLocation( 16.5, 56.5 );
        matrixGroup = new Group();
        matrixGroup.setName("Matrix");
        matrixGroup.addPerson(neo);
        matrixGroup.addPerson(cypher);
        matrixGroup.addPerson(trinity);

        repo.save(Arrays.asList(cypher, trinity, neo));
        groupRepo.save(matrixGroup);
        return this;
    }

}