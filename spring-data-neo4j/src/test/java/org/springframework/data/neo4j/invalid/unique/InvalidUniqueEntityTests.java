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
package org.springframework.data.neo4j.invalid.unique;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.neo4j.unique.domain.Club;
import org.springframework.data.neo4j.unique.domain.UniqueClub;
import org.springframework.data.neo4j.unique.domain.UniqueNumericIdClub;
import org.springframework.data.neo4j.unique.repository.ClubRepository;
import org.springframework.data.neo4j.unique.repository.UniqueClubRepository;
import org.springframework.data.neo4j.unique.repository.UniqueNumericIdClubRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertEquals;

public class InvalidUniqueEntityTests {

    @Test(expected = BeanCreationException.class)
    public void shouldThrowExceptionWhenMultipleUniqueSpecified() {
        ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("classpath:invalid-unique-test-context.xml");
        InvalidClubRepository invalidClubRepository = ctx.getBean(InvalidClubRepository.class);
        InvalidClub club = new InvalidClub();
        invalidClubRepository.save(club);
    }
}
