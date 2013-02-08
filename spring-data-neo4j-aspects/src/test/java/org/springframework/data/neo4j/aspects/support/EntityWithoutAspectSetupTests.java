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
package org.springframework.data.neo4j.aspects.support;

import org.junit.Test;
import org.springframework.data.neo4j.aspects.Person;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EntityWithoutAspectSetupTests {
    @Test
    public void testEquals() throws Exception {
        final Person p1 = new Person();
        final Person p2 = new Person();
        assertEquals(p1, p1);
        assertEquals(false, p1.equals(p2));
    }
    @Test
    public void testHashCode() throws Exception {
        final Person p1 = new Person();
        final Person p2 = new Person();
        assertTrue(p1.hashCode() > 0);
        assertEquals(false, p1.hashCode() == p2.hashCode());
    }
}
