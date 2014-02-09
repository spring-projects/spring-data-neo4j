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

import org.springframework.data.neo4j.repository.GraphRepository;

import java.util.Collection;
import java.util.Date;

import static org.springframework.data.neo4j.repository.query.AbstractDerivedFinderMethodTestBase.Thing;

public interface ThingRepository extends GraphRepository<AbstractDerivedFinderMethodTestBase.Thing> {
    Thing findByFirstNameAndLastName(String firstName, String lastName);
    Thing findByFirstName(String firstName);
    Thing findByDescription(String firstName);
    Thing findByDescriptionAndFirstName(String description,String firstName);
    Thing findByFirstNameAndDescription(String firstName,String description);
    Thing findByAge(int age);
    Thing findByAgeAndFirstName(int age,String firstName);
    Thing findByFirstNameLike(String firstName);
    Thing findByFirstNameContains(String firstName);
    Thing findByFirstNameEndsWith(String firstName);
    Thing findByFirstNameStartsWith(String firstName);
    Thing findByNumber(int number);
    
    Thing findByName(String name);
    Thing findByNameStartsWith(String name);
    Thing findByNameEndsWith(String name);
    Thing findByNameContains(String name);
    
    Thing findByNameLike(String name);
    Thing findByNameNotLike(String name);
    Thing findByNameMatches(String name);
    Thing findByTaggedIsTrue();
    Thing findByTaggedIsFalse();
    
    Thing findByNameExists();
    Thing findByNameIn(Collection<String> values);
    Thing findByNameNotIn(Collection<String> values);
    
    Thing findByBornBefore(Date date);
    Thing findByBornAfter(Date date);
    Thing findById(long id);
    Thing findByOwnerId(long id);

    // Label based indexes
    Thing findByAlias(String alias);
}
