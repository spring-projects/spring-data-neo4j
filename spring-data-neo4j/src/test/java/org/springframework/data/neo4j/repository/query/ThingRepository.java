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
    public DerivedFinderMethodTest.Thing findByFirstNameAndLastName(String firstName, String lastName);
    public DerivedFinderMethodTest.Thing findByFirstName(String firstName);
    public DerivedFinderMethodTest.Thing findByDescription(String firstName);
    public DerivedFinderMethodTest.Thing findByDescriptionAndFirstName(String description,String firstName);
    public DerivedFinderMethodTest.Thing findByFirstNameAndDescription(String firstName,String description);
    public DerivedFinderMethodTest.Thing findByAge(int age);
    public DerivedFinderMethodTest.Thing findByAgeAndFirstName(int age,String firstName);
    public DerivedFinderMethodTest.Thing findByFirstNameLike(String firstName);
    public DerivedFinderMethodTest.Thing findByFirstNameContains(String firstName);
    public DerivedFinderMethodTest.Thing findByFirstNameEndsWith(String firstName);
    public DerivedFinderMethodTest.Thing findByFirstNameStartsWith(String firstName);

    public DerivedFinderMethodTest.Thing findByName(String name);
    public DerivedFinderMethodTest.Thing findByNameStartsWith(String name);
    public DerivedFinderMethodTest.Thing findByNameEndsWith(String name);
    public DerivedFinderMethodTest.Thing findByNameContains(String name);

    public DerivedFinderMethodTest.Thing findByNameLike(String name);
    public DerivedFinderMethodTest.Thing findByNameNotLike(String name);
    public DerivedFinderMethodTest.Thing findByNameMatches(String name);
    public DerivedFinderMethodTest.Thing findByTaggedIsTrue();
    public DerivedFinderMethodTest.Thing findByTaggedIsFalse();

    public DerivedFinderMethodTest.Thing findByNameExists();
    public DerivedFinderMethodTest.Thing findByNameIn(Collection<String> values);
    public DerivedFinderMethodTest.Thing findByNameNotIn(Collection<String> values);

    public DerivedFinderMethodTest.Thing findByBornBefore(Date date);
    public DerivedFinderMethodTest.Thing findByBornAfter(Date date);
}
