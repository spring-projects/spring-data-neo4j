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
package org.springframework.data.neo4j.annotation.relatedto;

import org.springframework.data.neo4j.annotation.IdentifiableEntity;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;

@NodeEntity
public class Warchief extends IdentifiableEntity {
    private String name;

    @RelatedTo(type = "succeeds")
    private Warchief mentor;

    @RelatedTo(type = "is_advised_by")
    private Set<Advisor> advisors = new HashSet<Advisor>();

    public Warchief() {

    }

    public Warchief( String name ) {
        this.name = name;
    }

    public void succeeds( Warchief mentor ) {
        this.mentor = mentor;
    }

    public Warchief getMentor() {
        return mentor;
    }

    public void hasAdvisors(Advisor... advisors) {
        this.advisors.addAll(asList(advisors));
    }

    public Set<Advisor> getAdvisors() {
        return advisors;
    }
}
