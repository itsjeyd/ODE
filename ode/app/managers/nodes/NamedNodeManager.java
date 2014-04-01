package managers.nodes;

import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.F.Promise;

import models.nodes.LabeledNodeWithProperties;


public class NamedNodeManager extends LabeledNodeWithPropertiesManager {

    public static Promise<Boolean> updateName(LabeledNodeWithProperties node,
                                              String newName) {
        ObjectNode newProps = node.jsonProperties.deepCopy();
        newProps.put("name", newName);
        return LabeledNodeWithPropertiesManager
            .updateProperties(node, newProps);
    }

}
