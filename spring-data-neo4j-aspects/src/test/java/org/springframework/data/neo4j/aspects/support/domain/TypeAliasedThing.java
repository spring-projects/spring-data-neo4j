package org.springframework.data.neo4j.aspects.support.domain;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.Labels;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.support.index.IndexType;

@NodeEntity
@TypeAlias(value = "TypeAliasedThing")
public class TypeAliasedThing {

    String name;

    @Indexed(indexType = IndexType.SIMPLE)
    String legacyIndexedThingName;

    @Indexed(indexType = IndexType.LABEL)
    String schemaIndexedCommonName;

    @Indexed(indexType = IndexType.LABEL)
    String schemaIndexedThingName;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getLegacyIndexedThingName() {
        return legacyIndexedThingName;
    }

    public void setLegacyIndexedThingName(String legacyIndexedThingName) {
        this.legacyIndexedThingName = legacyIndexedThingName;
    }

    public String getSchemaIndexedThingName() {
        return schemaIndexedThingName;
    }

    public void setSchemaIndexedThingName(String schemaIndexedThingName) {
        this.schemaIndexedThingName = schemaIndexedThingName;
    }

    public String getSchemaIndexedCommonName() {
        return schemaIndexedCommonName;
    }

    public void setSchemaIndexedCommonName(String schemaIndexedCommonName) {
        this.schemaIndexedCommonName = schemaIndexedCommonName;
    }
}
