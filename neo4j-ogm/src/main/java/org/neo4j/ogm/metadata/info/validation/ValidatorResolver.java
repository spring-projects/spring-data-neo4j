package org.neo4j.ogm.metadata.info.validation;

import org.neo4j.ogm.annotation.NodeEntity;
import org.neo4j.ogm.annotation.RelationshipEntity;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * The ValidatorResolver returns Validator for specific Class Type.
 *
 */

public class ValidatorResolver {
    private Map<String, EntityValidator> validators = new HashMap<>();

    public ValidatorResolver() {
        validators.put(NodeEntity.CLASS, new NodeEntityValidator());
        validators.put(RelationshipEntity.CLASS, new RelationshipEntityValidator());
    }

    public EntityValidator getValidator(String classTypeName) {
        return validators.get(classTypeName);
    }
}