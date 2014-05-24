package managers.relationships;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import managers.relationships.AllowsManager;
import models.nodes.LabeledNodeWithProperties;
import neo4play.RelationshipService;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;


public class AllowsManager extends TypedRelManager {

    public AllowsManager() {
        this.type = "ALLOWS";
    }

}
