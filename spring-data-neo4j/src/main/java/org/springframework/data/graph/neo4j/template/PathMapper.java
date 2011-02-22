package org.springframework.data.graph.neo4j.template;

import org.neo4j.graphdb.Path;

/**
 * @author mh
 * @since 19.02.11
 */
public interface PathMapper<T> {

    T mapPath(Path path);

    public abstract class WithoutResult implements PathMapper<Void>, IterationController{
        public abstract void eachPath(Path path);

        @Override
        public Void mapPath(Path path) {
            eachPath(path);
            return null;
        }

        @Override
        public IterationMode getIterationMode() {
            return IterationMode.EAGER;
        }
    }
}
