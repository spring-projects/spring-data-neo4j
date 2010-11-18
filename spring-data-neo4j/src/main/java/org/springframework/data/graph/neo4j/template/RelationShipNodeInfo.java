/*
 * Copyright 2010 the original author or authors.
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

import org.neo4j.graphdb.RelationshipType;

class RelationShipNodeInfo
{
    private final RelationshipType relationshipType;
    private final NodeInfo nodeInfo;

    RelationShipNodeInfo(final RelationshipType relationshipType, final NodeInfo nodeInfo)
    {
        this.relationshipType = relationshipType;
        this.nodeInfo = nodeInfo;
    }

    public boolean equals(final Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final RelationShipNodeInfo that = (RelationShipNodeInfo) o;

        return nodeInfo.equals(that.nodeInfo) && relationshipType.name().equals(that.relationshipType.name());

    }

    public int hashCode()
    {
        return relationshipType.name().hashCode() * 31 + nodeInfo.hashCode();
    }

    public NodeInfo getNodeInfo()
    {
        return nodeInfo;
    }

    public RelationshipType getRelationshipType()
    {
        return relationshipType;
    }
}
