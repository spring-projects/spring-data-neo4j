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

public interface ThingRepository extends GraphRepository<DerivedFinderMethodTest.Thing> {
    DerivedFinderMethodTest.Thing findByFirstNameAndLastName(String firstName, String lastName);
    DerivedFinderMethodTest.Thing findByFirstName(String firstName);
    DerivedFinderMethodTest.Thing findByDescription(String firstName);
    DerivedFinderMethodTest.Thing findByDescriptionAndFirstName(String description,String firstName);
    DerivedFinderMethodTest.Thing findByFirstNameAndDescription(String firstName,String description);
    DerivedFinderMethodTest.Thing findByAge(int age);
    DerivedFinderMethodTest.Thing findByAgeAndFirstName(int age,String firstName);
    DerivedFinderMethodTest.Thing findByFirstNameLike(String firstName);
    DerivedFinderMethodTest.Thing findByFirstNameContains(String firstName);
    DerivedFinderMethodTest.Thing findByFirstNameEndsWith(String firstName);
    DerivedFinderMethodTest.Thing findByFirstNameStartsWith(String firstName);
    DerivedFinderMethodTest.Thing findByNumber(int number);
    
    DerivedFinderMethodTest.Thing findByName(String name);
    DerivedFinderMethodTest.Thing findByNameStartsWith(String name);
    DerivedFinderMethodTest.Thing findByNameEndsWith(String name);
    DerivedFinderMethodTest.Thing findByNameContains(String name);
    
    DerivedFinderMethodTest.Thing findByNameLike(String name);
    DerivedFinderMethodTest.Thing findByNameNotLike(String name);
    DerivedFinderMethodTest.Thing findByNameMatches(String name);
    DerivedFinderMethodTest.Thing findByTaggedIsTrue();
    DerivedFinderMethodTest.Thing findByTaggedIsFalse();
    
    DerivedFinderMethodTest.Thing findByNameExists();
    DerivedFinderMethodTest.Thing findByNameIn(Collection<String> values);
    DerivedFinderMethodTest.Thing findByNameNotIn(Collection<String> values);
    
    DerivedFinderMethodTest.Thing findByBornBefore(Date date);
    DerivedFinderMethodTest.Thing findByBornAfter(Date date);
    DerivedFinderMethodTest.Thing findById(long id);
    DerivedFinderMethodTest.Thing findByOwnerId(long id);
}
