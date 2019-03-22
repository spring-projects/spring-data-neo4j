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
package org.springframework.data.neo4j.repository.query;

import org.neo4j.helpers.collection.IteratorUtil;
import org.springframework.data.neo4j.fieldaccess.PropertyConverter;
import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.parser.Part;
import org.neo4j.index.lucene.ValueContext;

import java.util.*;

/**
 * Representation of a Cypher {@literal start} clause.
 *
 * @author Oliver Gierke
 */
abstract class StartClause {

    protected final SortedMap<Integer,PartInfo> partInfos=new TreeMap<Integer, PartInfo> ();

    /**
     * Creates a new {@link StartClause} from the given {@link PartInfo}
     */
    public StartClause(PartInfo partInfo) {
        this.partInfos.put(partInfo.getParameterIndex(), partInfo);
    }

    /**
     * Returns true if this start clause comprises of multiple parts
     */
    protected boolean hasMultipleParts() {
        return partInfos.size() > 1;
    }

    public abstract Map<Parameter, Object> resolveParameters(Map<Parameter, Object> parameters, Neo4jTemplate template);

    /**
     * Utility method which returns the primary (first) partInfo. In most cases
     * there will only actually ever be one.
     */
    public PartInfo getPartInfo() {
        return IteratorUtil.first(partInfos.values());
    }

    /**
     * Determines if it is possible to merge the provided PartInfo into
     * this existing start clause, AND if so, also adds it to the list of
     * parts managed by this clause.
     * Merging will only occur if the provided partInfo refers to the same
     * identifier as all all of the other parts contained in this start
     * clause AND it also refers to the same index.
     *
     * @param partInfo
     * @return true if the merge occurred otherwise false
     */
    public boolean merge(PartInfo partInfo) {
        for (PartInfo info : partInfos.values()) {
            if (info.sameIdentifier(partInfo) && info.sameIndex(partInfo)) {
                continue;
            }
            return false;
        }
        this.partInfos.put(partInfo.getParameterIndex(), partInfo);
        return true;
    }

    public boolean sameIdentifier(PartInfo info) {
        for (PartInfo partInfo : partInfos.values()) {
            if (!partInfo.sameIdentifier(info)) return false;
        }
        return true;
    }

    public boolean sameIndex(PartInfo info) {
        for (PartInfo partInfo : partInfos.values()) {
            if (!partInfo.sameIndex(info)) return false;
        }
        return true;
    }

    protected Collection<PartInfo> getPartInfos() {
        return partInfos.values();
    }
}


