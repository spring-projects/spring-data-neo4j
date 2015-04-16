package org.neo4j.ogm.metadata.info.validation;

import org.neo4j.ogm.annotation.EndNode;
import org.neo4j.ogm.annotation.RelationshipEntity;
import org.neo4j.ogm.annotation.StartNode;
import org.neo4j.ogm.annotation.Transient;
import org.neo4j.ogm.metadata.info.ClassInfo;

/**
 *
 * The Validator should be integration point between
 * DomainInfo and validation rules.
 *
 */

public class RelationshipEntityValidator extends EntityValidator {

    public void validate(ClassInfo classInfo) {
        clazz = new ClassValidator(new ClassValidatorInfo(classInfo));

        onlyOneStartNodeAndEndNodeAreAllowed();
        transientAndRelationshipAreForbidden();
        combinationRelationshipStartNodeEndNodeRequired();
        startNodeAndEndNodeMustBeOnDifferentFields();
    }

    private void onlyOneStartNodeAndEndNodeAreAllowed() {
        if(clazz.Annotations(RelationshipEntity.CLASS).Required()) {
            validate(clazz.Fields().Annotations(StartNode.CLASS).Unique() && clazz.Fields().Annotations(EndNode.CLASS).Unique(),
                    "Only one StartNode or EndNode is Allowed at RelationshipEntity.");
        }
    }

    private void transientAndRelationshipAreForbidden() {
        validate(clazz.Annotations(RelationshipEntity.CLASS, Transient.CLASS).Forbidden(),
                "Transient and RelationshipEntity are in conflict.");
    }

    private void combinationRelationshipStartNodeEndNodeRequired() {
        validate(!(clazz.Annotations(RelationshipEntity.CLASS).Required() ^ (clazz.Fields().Annotations(StartNode.CLASS, EndNode.CLASS).Required())),
                "RelationshipEntity must be companioned with StartNode and EndNode and vice versa.");
    }

    private void startNodeAndEndNodeMustBeOnDifferentFields() {
        if(clazz.Fields().Annotations(StartNode.CLASS).Required() && clazz.Fields().Annotations(EndNode.CLASS).Required()) {
            validate(clazz.Fields().Annotations(StartNode.CLASS, EndNode.CLASS).Unique(),
                    "StartNode and EndNode must be on separate fields.");
        }
    }

    private void validate(boolean result, String message, Object... messageParameters) {
        if(!result) {
            areValid = false;
            String formattedMessage = String.format(message, messageParameters);
            errorMessage.append(String.format("%s - %s", clazz.getName(), formattedMessage));
            errorMessage.append(System.getProperty("line.separator"));
        }
    }
}
