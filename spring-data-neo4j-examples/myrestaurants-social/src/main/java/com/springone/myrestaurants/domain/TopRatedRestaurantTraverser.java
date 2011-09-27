package com.springone.myrestaurants.domain;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.traversal.BranchOrderingPolicy;
import org.neo4j.graphdb.traversal.BranchSelector;
import org.neo4j.graphdb.traversal.TraversalBranch;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.kernel.impl.traversal.TraversalDescriptionImpl;
import org.springframework.data.neo4j.core.FieldTraversalDescriptionBuilder;
import org.springframework.data.neo4j.core.NodeBacked;
import org.springframework.data.neo4j.mapping.Neo4JPersistentProperty;

import java.lang.reflect.Field;

/**
 * @author Michael Hunger
 * @since 02.10.2010
 * TODO: calculate a rating assigned to each restaurant that is the sum of its stars
 * the stars are multiplied by pow(1/2,distance to startNode==path.depth)
 */
class TopRatedRestaurantTraverser implements FieldTraversalDescriptionBuilder {
    @Override
    public TraversalDescription build(NodeBacked start, Neo4JPersistentProperty field, String... params) {
        return new TraversalDescriptionImpl()
                .breadthFirst()
                .relationships(DynamicRelationshipType.withName("friends"))
                .order(new BranchOrderingPolicy() {
                    @Override
                    public BranchSelector create(final TraversalBranch startBranch) {
                        return new BranchSelector(){
                            @Override
                            public TraversalBranch next() {
                                TraversalBranch branch=startBranch;
                                while (branch!=null) {
                                    if (branch.depth()<branch.next().depth()) return branch;
                                    branch=branch.next();
                                }
                                return null;
                            }
                        };
                    }
                });
    }

}
