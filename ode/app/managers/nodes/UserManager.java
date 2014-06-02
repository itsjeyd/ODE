package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import constants.NodeType;
import play.libs.F.Promise;


public class UserManager extends LabeledNodeWithPropertiesManager {

    public UserManager() {
        this.label = NodeType.USER.toString();
    }

    // CREATE

    @Override
    protected Promise<Boolean> create(JsonNode properties, String location) {
        return super.create(properties, location, "username");
    }

}
