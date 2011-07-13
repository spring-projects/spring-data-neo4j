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

package org.springframework.data.neo4j.support.path;

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
    public abstract class WithoutResult implements PathMapper<Void>, IterationController {
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
