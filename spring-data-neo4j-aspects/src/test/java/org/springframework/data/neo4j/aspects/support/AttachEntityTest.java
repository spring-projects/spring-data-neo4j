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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.aspects.Developer;
import org.springframework.data.neo4j.aspects.core.NodeBacked;
import org.springframework.data.neo4j.repository.DirectGraphRepositoryFactory;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:org/springframework/data/neo4j/aspects/support/Neo4jGraphPersistenceTest-context.xml"})

public class AttachEntityTest {

    protected final Log log = LogFactory.getLog(getClass());

    @Autowired
    private DirectGraphRepositoryFactory graphRepositoryFactory;

    @Test
    @Transactional
    public void entityShouldHaveNoNode() {
        Developer dev = new Developer("Michael");
        assertFalse(hasPersistentState(dev));
        assertNull(nodeFor(dev));
    }

    private boolean hasPersistentState(NodeBacked nodeBacked) {
        return nodeBacked.hasPersistentState();
    }

    private Node nodeFor(NodeBacked nodeBacked) {
        return nodeBacked.getPersistentState();
    }


}
