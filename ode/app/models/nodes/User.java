package models.nodes;

import constants.NodeType;
import managers.nodes.UserManager;


public class User extends LabeledNodeWithProperties {

    public static final UserManager nodes = new UserManager();

    public String username;
    public String password;

    private User() {
        super(NodeType.USER);
    }

}
