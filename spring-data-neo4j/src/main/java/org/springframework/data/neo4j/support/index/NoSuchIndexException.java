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
package org.springframework.data.neo4j.support.index;

import org.springframework.dao.DataRetrievalFailureException;

/**
* @author mh
* @since 16.10.11
*/
public class NoSuchIndexException extends DataRetrievalFailureException {
    private static final long serialVersionUID = -5708027180371353224L;

    private final String index;

    public NoSuchIndexException(String index) {
        super("No such index: "+index);
        this.index = index;
    }

    public String getIndex() {
        return index;
    }
}
