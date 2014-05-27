package managers.relationships;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.F.Tuple;
import play.mvc.Http.Status;

import constants.RelationshipType;
import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import managers.functions.RelationshipCreatedFunction;
import models.nodes.AVM;
import models.nodes.Feature;
import models.nodes.Rule;
import models.relationships.HasValueRelationship;


public class HasValueRelationshipManager extends HasRelationshipManager {

    public static Promise<JsonNode> getEndNode(
        final Feature startNode, Rule rule, AVM parent) {
        Promise<UUID> ruleUUID = rule.getUUID();
        Promise<UUID> avmUUID = parent.getUUID();
        Promise<Tuple<UUID, UUID>> uuids = ruleUUID.zip(avmUUID);
        Promise<WS.Response> response = uuids.flatMap(
            new Function<Tuple<UUID, UUID>, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(Tuple<UUID, UUID> uuids) {
                    ObjectNode relProps = Json.newObject();
                    relProps.put("rule", uuids._1.toString());
                    relProps.put("avm", uuids._2.toString());
                    return Neo4jService.getRelationshipTarget(
                        startNode, RelationshipType.HAS, relProps);
                }
            });
        return response.map(new JsonFunction());
    }

    public static Promise<Boolean> delete(
        final Feature startNode, Rule rule, AVM parent) {
        Promise<UUID> ruleUUID = rule.getUUID();
        Promise<UUID> avmUUID = parent.getUUID();
        Promise<Tuple<UUID, UUID>> uuids = ruleUUID.zip(avmUUID);
        Promise<WS.Response> response = uuids.flatMap(
            new Function<Tuple<UUID, UUID>, Promise<WS.Response>>() {
                public Promise<WS.Response> apply(Tuple<UUID, UUID> uuids) {
                    ObjectNode data = Json.newObject();
                    data.put("rule", uuids._1.toString());
                    data.put("avm", uuids._2.toString());
                    return Neo4jService
                        .deleteTypedRelationshipWithProperties(
                            startNode, RelationshipType.HAS, data);
                }
            });
        return response.map(
            new Function<WS.Response, Boolean>() {
                public Boolean apply(WS.Response response) {
                    return response.getStatus() == Status.OK;
                }
            });
    }

}
