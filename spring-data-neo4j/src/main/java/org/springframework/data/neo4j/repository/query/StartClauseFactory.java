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

import org.springframework.data.neo4j.mapping.Neo4jPersistentProperty;
import org.springframework.data.repository.query.parser.Part;

import java.util.EnumSet;
import java.util.List;

/**
 * This class is responsible for creating appropriate StartClause
 * instances depending on the part(s) it is being created from.
 *
 * @author Nicki Watt
 */
public class StartClauseFactory {

    /**
     * At present, Multiple parts are always assumed to result in a
     * a FullTextIndexBasedStartClause
     * @param partInfos The various parts which the start clause
     *                  needs to be created around/for.
     * @return A appropriate StartClause
     */
    public static StartClause create(List<PartInfo> partInfos) {
        return (partInfos.size() == 1)
            ? create(partInfos.get(0))
            : new FullTextIndexBasedStartClause(partInfos);
    }

    /**
     * Given a particular partInfo, create an appropriate StartClause
     * instance
     *
     * @param partInfo The part which the start clause needs to be
     *                 created around/for.
     * @return A appropriate StartClause
     */
    public static StartClause create(PartInfo partInfo) {
        if (partInfo.isIndexed()) {
            return (partInfo.isFullText() || isTextualSearchLikePart(partInfo))
                ? new FullTextIndexBasedStartClause(partInfo)
                : new ExactIndexBasedStartClause(partInfo);
        }

        Neo4jPersistentProperty leafProperty = partInfo.getLeafProperty();
        if (leafProperty.isRelationship() || leafProperty.isIdProperty()) {
            return new GraphIdStartClause(partInfo);
        }

        throw new IllegalArgumentException("Cannot determine an appropriate Start Clause for partInfo=" + partInfo );
    }

    private static boolean isTextualSearchLikePart(PartInfo partInfo) {
        return EnumSet.of(Part.Type.LIKE,Part.Type.STARTING_WITH,Part.Type.CONTAINING,Part.Type.ENDING_WITH).contains(partInfo.getType());
    }
}
