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

import java.util.HashSet;
import java.util.Set;

@NodeEntity
public class TubeStation
{
    @GraphId
    private Long id;

    private String name;

    @RelatedToVia
    private Set<Line> lines = new HashSet<Line>();

    public TubeStation()
    {

    }

    TubeStation( String name )
    {
        this.name = name;
    }

    public Long getId()
    {
        return id;
    }

    public void connectsTo( TubeStation tubeStation, String line )
    {
        lines.add( new Line( this, tubeStation, line ) );
    }

    public Set<Line> getLines()
    {
        return lines;
    }

    public String getName()
    {
        return name;
    }
}
