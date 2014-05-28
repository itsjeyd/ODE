package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F.Promise;

import models.nodes.User;


public class UserManager extends LabeledNodeWithPropertiesManager {

    public UserManager() {
        this.label = "User";
    }

    @Override
    protected Promise<Boolean> create(JsonNode properties, String location) {
        return super.create(properties, location, "username");
    }

    public static Promise<Boolean> create(User user) {
        user.jsonProperties.put("password", user.password);
        return LabeledNodeWithPropertiesManager.create(user);
    }

}
