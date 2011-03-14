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
