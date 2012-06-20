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
package org.springframework.data.neo4j.annotation;

@RelationshipEntity(type = "unrelated")
public class PerpetualRelations
{
    @GraphId
    private Long id;

    @StartNode
    private SuperState ss1;

    @EndNode
    private SuperState ss2;

    @RelationshipType
    private String status;

    public PerpetualRelations()
    {
    }

    public PerpetualRelations( SuperState ss1, SuperState ss2 )
    {
        this.ss1 = ss1;
        this.ss2 = ss2;
    }

    public PerpetualRelations( SuperState ss1, SuperState ss2, String status )
    {
        this(ss1, ss2 );
        this.status = status;
    }
}
