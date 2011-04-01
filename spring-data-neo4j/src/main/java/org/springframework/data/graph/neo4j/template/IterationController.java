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

package org.springframework.data.graph.neo4j.template;

/**
 * If the @{see PathMapper} implements this interface the returned @{see IterationMode}
 * is considered for the path mapping operation.
 * @author mh
 * @since 22.02.11
 */
public interface IterationController {
    public enum IterationMode {
        /**
         * doesn't evaluate before the iterable is accessed
         */
        LAZY,
        /**
         * iterates eagerly over the results to ensure callbacks happening and also returns the full results
         */
        EAGER,
        /**
         * iterates eagerly over the results to ensure callbacks happening, stops iteration on first encountered
         * null value, useful for aborting iterations early
         */
        EAGER_STOP_ON_NULL,
        /**
         * iterates eagerly over the results to ensure callbacks happening, ignores results and returns null from
         * the iteration, i.e. callback only
         */
        EAGER_IGNORE_RESULTS
    }

    IterationMode getIterationMode();
}
