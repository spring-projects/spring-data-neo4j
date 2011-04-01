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

package org.springframework.data.graph.neo4j.fieldaccess;

import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.core.NodeBacked;

import java.util.*;

/**
 * @author mh
 * @since 12.03.11
 */
public class BackReferences {
    private List<NodeBacked> backrefs=new ArrayList<NodeBacked>();
    private EntityState<?, ?> entityState;

    public BackReferences(EntityState<?,?> entityState) {

        this.entityState = entityState;
    }

    public void addBackReferences(Collection<NodeBacked> backReference) {
        this.backrefs.addAll(backReference);
    }


    private void pruneInvalidBackRefs() {
        GraphBacked entity = entityState.getEntity();
        for (Iterator<NodeBacked> it = backrefs.iterator(); it.hasNext();) {
            NodeBacked backRef = it.next();
            if (backRef.refersTo(entity)) continue;
            it.remove();
        }
    }


    public void persistNeighbours() {
        pruneInvalidBackRefs();
        for (NodeBacked backref : backrefs) {
            backref.persist();
        }
    }
}
