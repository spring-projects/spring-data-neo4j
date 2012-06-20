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

@NodeEntity
public class Country extends IdentifiableEntity
{
    private String name;

    @RelatedToVia
    private InterCountryRelationship acquaintance;

    @RelatedToVia(type = "special")
    private InterCountryRelationship friend;

    public Country()
    {
    }

    public Country( String name )
    {
        this.name = name;
    }

    public void hasCordialRelationsWith( Country country, String description )
    {
        acquaintance = new InterCountryRelationship(this, country, description );
    }

    public void hasSpecialRelationsWith( Country country )
    {
        friend = new InterCountryRelationship( this, country, "The Special Relationship" );
    }
}
