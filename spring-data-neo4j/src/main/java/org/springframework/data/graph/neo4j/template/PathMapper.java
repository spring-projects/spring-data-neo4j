package org.springframework.data.graph.neo4j.template;

import org.neo4j.graphdb.Path;

/**
 * A mapper for paths as the generic return type of querying graph operations. Simple results like just nodes
 * or relationships are also wrapped in a @{see Path} for uniform access.
 *
 * Allows iteration control when implementing @{see IterationController}. Default iteration mode is @{see IterationMode#LAZY}
 *
 * Inner class @{see PathMapper.WithoutResult} allows callbacks instead and comes with an eager iteration mode.
 * @see Path
 * @author mh
 * @since 19.02.11
 */
public interface PathMapper<T> {

    /**
     * map operation, converts the path to any other, specified type instance
     * @param path given path
     * @return mapped type instance
     */
    T mapPath(Path path);

    /**
     * callback instead of mapping
     */
    public abstract class WithoutResult implements PathMapper<Void>, IterationController{
        public abstract void eachPath(Path path);

        @Override
        public Void mapPath(Path path) {
            eachPath(path);
            return null;
        }

        @Override
        public IterationMode getIterationMode() {
            return IterationMode.EAGER_IGNORE_RESULTS;
        }
    }
}
