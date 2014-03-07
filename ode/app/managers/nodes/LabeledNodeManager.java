package managers.nodes;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Promise;

import constants.NodeType;
import managers.functions.NodeListFunction;
import neo4play.Neo4jService;


public class LabeledNodeManager {

    protected static Promise<List<JsonNode>> all(NodeType type) {
        Promise<WS.Response> response = Neo4jService.getNodesByLabel(
            type.toString());
        return response.map(new NodeListFunction());
    }

}
