package managers.relationships;

import constants.RelationshipType;
import managers.relationships.AllowsManager;


public class AllowsManager extends TypedRelManager {

    public AllowsManager() {
        this.type = RelationshipType.ALLOWS.toString();
    }

}
