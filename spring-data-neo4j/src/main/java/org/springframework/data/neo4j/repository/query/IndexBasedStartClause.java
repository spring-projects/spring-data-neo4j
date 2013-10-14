/**
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.neo4j.repository.query;

/**
 * Abstract class which represents a start clause which makes
 * use of an indexof some sort.
 *
 * @author Nicki Watt
 */
abstract class IndexBasedStartClause extends StartClause {

    public IndexBasedStartClause(PartInfo partInfo) {
        super(partInfo);
    }


}
