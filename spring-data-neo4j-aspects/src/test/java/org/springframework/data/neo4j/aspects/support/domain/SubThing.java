package org.springframework.data.neo4j.aspects.support.domain;

import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.support.index.IndexType;

@NodeEntity
public class SubThing extends Thing {

    @Indexed(indexType = IndexType.SIMPLE)
    String legacyIndexedSubThingName;

    @Indexed(indexType = IndexType.LABEL)
    String schemaIndexedSubThingName;

    public String getLegacyIndexedSubThingName() {
        return legacyIndexedSubThingName;
    }

    public void setLegacyIndexedSubThingName(String legacyIndexedSubThingName) {
        this.legacyIndexedSubThingName = legacyIndexedSubThingName;
    }

    public String getSchemaIndexedSubThingName() {
        return schemaIndexedSubThingName;
    }

    public void setSchemaIndexedSubThingName(String schemaIndexedSubThingName) {
        this.schemaIndexedSubThingName = schemaIndexedSubThingName;
    }
}

