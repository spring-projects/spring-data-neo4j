package org.springframework.data.graph.neo4j.template;

import org.neo4j.graphdb.Path;
import org.neo4j.helpers.collection.IterableWrapper;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.data.graph.neo4j.template.IterationController.IterationMode.EAGER_STOP_ON_NULL;

/**
 * @author mh
 * @since 22.02.11
 */
public class PathMappingIterator {
    <T> Iterable<T> mapPaths(final Iterable<Path> paths, final PathMapper<T> pathMapper) {
        assert paths != null;
        assert pathMapper != null;
        IterationController.IterationMode mode = getIterationControl(pathMapper);
        switch (mode) {
            case EAGER:
            case EAGER_STOP_ON_NULL:
                List<T> result = new ArrayList<T>();
                for (Path path : paths) {
                    T mapped = pathMapper.mapPath(path);
                    if (mapped == null && mode == EAGER_STOP_ON_NULL) break;
                    result.add(mapped);
                }
                return result;

            case EAGER_IGNORE_RESULTS:
                for (Path path : paths) {
                    pathMapper.mapPath(path);
                }
                return null;
            case LAZY:
                return new IterableWrapper<T, Path>(paths) {
                    @Override
                    protected T underlyingObjectToObject(Path path) {
                        return pathMapper.mapPath(path);
                    }
                };
            default:
                throw new IllegalStateException("Unknown IterationControl " + mode);
        }
    }

    private <T> IterationController.IterationMode getIterationControl(PathMapper<T> pathMapper) {
        if (pathMapper instanceof IterationController) {
            IterationController.IterationMode result = ((IterationController) pathMapper).getIterationMode();
            if (result != null) return result;
        }
        return IterationController.IterationMode.LAZY;
    }
}
