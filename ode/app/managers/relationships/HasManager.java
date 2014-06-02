package managers.relationships;

import constants.RelationshipType;


public class HasManager extends TypedRelManager {

    public HasManager() {
        this.type = RelationshipType.HAS.toString();
    }

}
