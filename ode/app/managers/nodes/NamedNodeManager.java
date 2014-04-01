package managers.nodes;

import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.WS;
import play.libs.F.Promise;

import neo4play.Neo4jService;
import managers.functions.UpdatedFunction;
import models.nodes.LabeledNodeWithProperties;


public class NamedNodeManager extends LabeledNodeWithPropertiesManager {

    public static Promise<Boolean> updateName(LabeledNodeWithProperties node,
                                              String newName) {
        ObjectNode newProps = node.jsonProperties.deepCopy();
        newProps.put("name", newName);
        Promise<WS.Response> response = Neo4jService.updateNodeProperties(
            node.getLabel(), node.jsonProperties, newProps);
        return response.map(new UpdatedFunction());
    }

}
