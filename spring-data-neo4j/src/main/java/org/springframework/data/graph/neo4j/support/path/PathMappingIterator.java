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

package org.springframework.data.graph.neo4j.support.path;

import org.neo4j.graphdb.Path;
import org.neo4j.helpers.collection.ClosableIterable;
import org.neo4j.helpers.collection.IterableWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mh
 * @since 22.02.11
 */
public class PathMappingIterator {
    public <T> Iterable<T> mapPaths(final Iterable<Path> paths, final PathMapper<T> pathMapper) {
        assert paths != null;
        assert pathMapper != null;
        IterationController.IterationMode mode = getIterationControl(pathMapper);
        switch (mode) {
            case EAGER:
            case EAGER_STOP_ON_NULL:
                List<T> result = new ArrayList<T>();
                try {
                    for (Path path : paths) {
                        T mapped = pathMapper.mapPath(path);
                        if (mapped == null && mode == IterationController.IterationMode.EAGER_STOP_ON_NULL) break;
                        result.add(mapped);
                    }
                } finally {
                    close(paths);
                }
                return result;

            case EAGER_IGNORE_RESULTS:
                try {
                    for (Path path : paths) {
                        pathMapper.mapPath(path);
                    }
                } finally {
                    close(paths);
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

    private void close(Iterable<Path> paths) {
        if (paths instanceof ClosableIterable) {
            ((ClosableIterable)paths).close();
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
