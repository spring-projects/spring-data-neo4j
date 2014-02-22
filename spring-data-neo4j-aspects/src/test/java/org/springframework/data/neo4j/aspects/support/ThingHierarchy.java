/**
 * Copyright 2014 the original author or authors.
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

package org.springframework.data.neo4j.aspects.support;

import org.neo4j.graphdb.Transaction;
import org.springframework.data.neo4j.aspects.support.domain.*;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.mapping.StoredEntityType;

/**
 * Create various flavours of "Things" for testing hierarchical
 * nodeEntity based functionality - specifically geared
 * towards testing Schema (aka Label based) indexing etc
 *
 * @author Nicki Watt
 * @since 22-02-2014
 */
public class ThingHierarchy {

    private final Neo4jTemplate neo4jTemplate;

    // Type Aliased ...
    public TypeAliasedThing typeAliasedThing;
    public TypeAliasedSubThing typeAliasedSubThing;
    public TypeAliasedSubSubThing typeAliasedSubSubThing;
    public StoredEntityType typeAliasedThingType;
    public StoredEntityType typeAliasedSubThingType;
    public StoredEntityType typeAliasedSubSubThingType;

    // Non Type Aliased ...
    public Thing thing;
    public SubThing subThing;
    public SubSubThing subSubThing;
    public StoredEntityType thingType;
    public StoredEntityType subThingType;
    public StoredEntityType subSubThingType;

    public ThingHierarchy(Neo4jTemplate template) {
        this.neo4jTemplate = template;
    }

    public void createThingHierarchyWithTypeAliases() {
        try (Transaction tx = neo4jTemplate.getGraphDatabaseService().beginTx()) {
            createTypeAliasedThing();
            createTypeAliasedSubThing();
            createTypeAliasedSubSubThing();
            tx.success();
        }
    }

    public void createThingHierarchyWithoutTypeAliases() {
        try (Transaction tx = neo4jTemplate.getGraphDatabaseService().beginTx()) {
            createThing();
            createSubThing();
            createSubSubThing();
            tx.success();
        }
    }

    protected void createTypeAliasedThing() {
        typeAliasedThing = new TypeAliasedThing();
        typeAliasedThing.setName("thing");
        typeAliasedThing.setLegacyIndexedThingName("thing-theLegacyIndexedThingName");
        typeAliasedThing.setSchemaIndexedThingName("thing-theSchemaIndexedThingName");
        typeAliasedThing.setSchemaIndexedCommonName("common");
        neo4jTemplate.save(typeAliasedThing);

        typeAliasedThingType = neo4jTemplate.getStoredEntityType(typeAliasedThing);
    }

    protected void createTypeAliasedSubThing() {
        typeAliasedSubThing = new TypeAliasedSubThing();
        typeAliasedSubThing.setName("subThing");
        typeAliasedSubThing.setLegacyIndexedThingName("subThing-theLegacyIndexedThingName");
        typeAliasedSubThing.setSchemaIndexedThingName("subThing-theSchemaIndexedThingName");
        typeAliasedSubThing.setLegacyIndexedSubThingName("subThing-theLegacyIndexedSubThingName");
        typeAliasedSubThing.setSchemaIndexedSubThingName("subThing-theSchemaIndexedSubThingName");
        typeAliasedSubThing.setSchemaIndexedCommonName("common");
        neo4jTemplate.save(typeAliasedSubThing);

        typeAliasedSubThingType = neo4jTemplate.getStoredEntityType(typeAliasedSubThing);
    }

    protected void createTypeAliasedSubSubThing() {
        typeAliasedSubSubThing = new TypeAliasedSubSubThing();
        typeAliasedSubSubThing.setName("subSubThing");
        typeAliasedSubSubThing.setLegacyIndexedThingName("subSubThing-theLegacyIndexedThingName");
        typeAliasedSubSubThing.setSchemaIndexedThingName("subSubThing-theSchemaIndexedThingName");
        typeAliasedSubSubThing.setLegacyIndexedSubThingName("subSubThing-theLegacyIndexedSubThingName");
        typeAliasedSubSubThing.setSchemaIndexedSubThingName("subSubThing-theSchemaIndexedSubThingName");
        typeAliasedSubSubThing.setLegacyIndexedSubSubThingName("subSubThing-theLegacyIndexedSubSubThingName");
        typeAliasedSubSubThing.setSchemaIndexedSubSubThingName("subSubThing-theSchemaIndexedSubSubThingName");
        typeAliasedSubSubThing.setSchemaIndexedCommonName("common");
        neo4jTemplate.save(typeAliasedSubSubThing);

        typeAliasedSubSubThingType = neo4jTemplate.getStoredEntityType(typeAliasedSubSubThing);
    }

    protected void createThing() {
        thing = new Thing();
        thing.setName("thing");
        thing.setLegacyIndexedThingName("thing-theLegacyIndexedThingName");
        thing.setSchemaIndexedThingName("thing-theSchemaIndexedThingName");
        thing.setSchemaIndexedCommonName("common");
        neo4jTemplate.save(thing);

        thingType = neo4jTemplate.getStoredEntityType(thing);
    }

    protected void createSubThing() {
        subThing = new SubThing();
        subThing.setName("subThing");
        subThing.setLegacyIndexedThingName("subThing-theLegacyIndexedThingName");
        subThing.setSchemaIndexedThingName("subThing-theSchemaIndexedThingName");
        subThing.setLegacyIndexedSubThingName("subThing-theLegacyIndexedSubThingName");
        subThing.setSchemaIndexedSubThingName("subThing-theSchemaIndexedSubThingName");
        subThing.setSchemaIndexedCommonName("common");
        neo4jTemplate.save(subThing);

        subThingType = neo4jTemplate.getStoredEntityType(subThing);
    }

    protected void createSubSubThing() {
        subSubThing = new SubSubThing();
        subSubThing.setName("subSubThing");
        subSubThing.setLegacyIndexedThingName("subSubThing-theLegacyIndexedThingName");
        subSubThing.setSchemaIndexedThingName("subSubThing-theSchemaIndexedThingName");
        subSubThing.setLegacyIndexedSubThingName("subSubThing-theLegacyIndexedSubThingName");
        subSubThing.setSchemaIndexedSubThingName("subSubThing-theSchemaIndexedSubThingName");
        subSubThing.setLegacyIndexedSubSubThingName("subSubThing-theLegacyIndexedSubSubThingName");
        subSubThing.setSchemaIndexedSubSubThingName("subSubThing-theSchemaIndexedSubSubThingName");
        subSubThing.setSchemaIndexedCommonName("common");
        neo4jTemplate.save(subSubThing);

        subSubThingType = neo4jTemplate.getStoredEntityType(subSubThing);
    }
}
