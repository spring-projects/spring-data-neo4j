package org.springframework.data.neo4j.aspects.support.domain;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.support.index.IndexType;

@NodeEntity
@TypeAlias(value = "TypeAliasedSubSubThing")
public class TypeAliasedSubSubThing extends TypeAliasedSubThing {

    @Indexed(indexType = IndexType.SIMPLE)
    String legacyIndexedSubSubThingName;

    @Indexed(indexType = IndexType.LABEL)
    String schemaIndexedSubSubThingName;

    public String getLegacyIndexedSubSubThingName() {
        return legacyIndexedSubSubThingName;
    }

    public void setLegacyIndexedSubSubThingName(String legacyIndexedSubSubThingName) {
        this.legacyIndexedSubSubThingName = legacyIndexedSubSubThingName;
    }

    public String getSchemaIndexedSubSubThingName() {
        return schemaIndexedSubSubThingName;
    }

    public void setSchemaIndexedSubSubThingName(String schemaIndexedSubSubThingName) {
        this.schemaIndexedSubSubThingName = schemaIndexedSubSubThingName;
    }
}
