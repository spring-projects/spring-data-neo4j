/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.support.index;

import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.ResourceIterator;
import org.neo4j.graphdb.index.IndexHits;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
* @author mh
* @since 16.10.11
*/
public class EmptyIndexHits<S extends PropertyContainer> implements IndexHits<S> {
    @Override
    public int size() {
        return 0;
    }

    @Override
    public void close() {
    }

    @Override
    public S getSingle() {
        return null;
    }

    @Override
    public float currentScore() {
        return 0;
    }

    @Override
    public ResourceIterator<S> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public S next() {
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}
