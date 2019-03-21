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
package org.springframework.data.neo4j.annotation.relatedtovia;

import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.GraphId;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.RelationshipType;
import org.springframework.data.neo4j.annotation.StartNode;

@RelationshipEntity(type = "notplaying")
public class PlayerStatus
{
    @GraphId
    private Long id;

    @StartNode
    private Team team;

    @EndNode
    private Player player;

    @RelationshipType
    private String status;

    public PlayerStatus()
    {

    }

    public PlayerStatus( Team team, Player player )
    {
        this.team = team;
        this.player = player;
    }

    public PlayerStatus( Team team, Player player, String status )
    {
        this( team, player );

        this.status = status;
    }
}
