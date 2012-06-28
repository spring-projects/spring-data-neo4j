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
package org.springframework.data.neo4j.annotation.relatedto;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.neo4j.annotation.IdentifiableEntity;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

@NodeEntity
public class Zoo extends IdentifiableEntity {
    private String name;

    @RelatedTo(type = "exhibit", enforceTargetType = true)
    private Iterable<Animal> animals;

    @RelatedTo(type = "exhibit", enforceTargetType = true)
    private Set<Herbivore> herbivores = new HashSet<Herbivore>();

    @RelatedTo(type = "exhibit", enforceTargetType = true)
    private Set<Carnivore> carnivores = new HashSet<Carnivore>();

    public Zoo() {

    }

    public Zoo( String name ) {
        this.name = name;
    }

    public void exhibits( Herbivore herbivore ) {
        herbivores.add(herbivore);
    }

    public void exhibits( Carnivore carnivore ) {
        carnivores.add(carnivore);
    }

    public Iterable<Animal> getAllAnimals() {
        return animals;
    }

    public Set<Herbivore> getHerbivores() {
        return herbivores;
    }

    public Set<Carnivore> getCarnivores() {
        return carnivores;
    }
}
