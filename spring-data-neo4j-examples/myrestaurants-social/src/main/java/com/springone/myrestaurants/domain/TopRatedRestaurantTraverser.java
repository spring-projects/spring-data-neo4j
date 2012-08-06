package com.springone.myrestaurants.domain;

import org.neo4j.graphdb.DynamicRelationshipType;
import org.neo4j.graphdb.PathExpander;
import org.neo4j.graphdb.traversal.*;
import org.neo4j.kernel.impl.traversal.TraversalDescriptionImpl;
import org.springframework.data.neo4j.core.FieldTraversalDescriptionBuilder;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;

/**
 * @author Michael Hunger
 * @since 02.10.2010
 * TODO: calculate a rating assigned to each restaurant that is the sum of its stars
 * the stars are multiplied by pow(1/2,distance to startNode==path.depth)
 */
class TopRatedRestaurantTraverser implements FieldTraversalDescriptionBuilder {
    @Override
    public TraversalDescription build(Object start, Neo4jPersistentProperty field, String... params) {
        return new TraversalDescriptionImpl()
                .breadthFirst()
                .relationships(DynamicRelationshipType.withName("friends"))
                .order(new BranchOrderingPolicy() {
                    @Override
                    public BranchSelector create(final TraversalBranch startBranch, final PathExpander expander) {
                        return new BranchSelector(){
                            @Override
                            public TraversalBranch next(TraversalContext metadata) {
                                TraversalBranch branch=startBranch;
                                while (branch!=null) {
                                    if (branch.length()<branch.next(expander,metadata).length()) return branch;
                                    branch=branch.next(expander,metadata);
                                }
                                return null;
                            }

                        };
                    }
                });
    }

}
