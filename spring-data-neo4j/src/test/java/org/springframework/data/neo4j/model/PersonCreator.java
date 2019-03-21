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

package org.springframework.data.neo4j.model;

import org.neo4j.graphdb.Node;
import org.springframework.data.persistence.StateBackedCreator;

/**
 * @author mh
 * @since 25.03.11
 */
public class PersonCreator implements StateBackedCreator<Person,Node> {
    @Override
    public Person create(Node n, Class<Person> c) throws Exception {
        return new Person(n);
    }
}
