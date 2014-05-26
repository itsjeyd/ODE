package managers.relationships;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import managers.functions.JsonFunction;
import managers.functions.SuccessFunction;
import models.nodes.LabeledNodeWithProperties;
import neo4play.RelationshipService;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;


public abstract class TypedRelManager extends RelManager {

    protected String type;

    public Promise<Boolean> exists(
        LabeledNodeWithProperties startNode,
        LabeledNodeWithProperties endNode) {
        Promise<JsonNode> json = get(startNode, endNode);
        return json.map(
            new Function<JsonNode, Boolean>() {
                public Boolean apply(JsonNode json) {
                    return json.get("data").size() > 0;
                }
            });
    }

    private Promise<JsonNode> get(
        LabeledNodeWithProperties startNode,
        LabeledNodeWithProperties endNode) {
        Promise<WS.Response> response = RelationshipService
            .getRelationship(startNode, endNode, this.type);
        return response.map(new JsonFunction());
    }

    public Promise<Boolean> create(
        final LabeledNodeWithProperties startNode,
        final LabeledNodeWithProperties endNode) {
        Promise<String> location = beginTransaction();
        Promise<Boolean> created = location.flatMap(
            new Function<String, Promise<Boolean>>() {
                public Promise<Boolean> apply(final String location) {
                    Promise<Boolean> created =
                        create(startNode, endNode, location);
                    return created.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean created) {
                                if (created) {
                                    return commitTransaction(location);
                                }
                                return Promise.pure(false);
                            }
                        });
                }
            });
        return created;
    }

    public Promise<Boolean> create(
        final LabeledNodeWithProperties startNode,
        final LabeledNodeWithProperties endNode, final JsonNode properties) {
        Promise<String> location = beginTransaction();
        Promise<Boolean> created = location.flatMap(
            new Function<String, Promise<Boolean>>() {
                public Promise<Boolean> apply(final String location) {
                    Promise<Boolean> created =
                        create(startNode, endNode, properties, location);
                    return created.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean created) {
                                if (created) {
                                    return commitTransaction(location);
                                }
                                return Promise.pure(false);
                            }
                        });
                }
            });
        return created;
    }

    public Promise<Boolean> delete(
        final LabeledNodeWithProperties startNode,
        final LabeledNodeWithProperties endNode) {
        Promise<String> location = beginTransaction();
        Promise<Boolean> deleted = location.flatMap(
            new Function<String, Promise<Boolean>>() {
                public Promise<Boolean> apply(final String location) {
                    Promise<Boolean> deleted =
                        delete(startNode, endNode, location);
                    return deleted.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean deleted) {
                                if (deleted) {
                                    return commitTransaction(location);
                                }
                                return Promise.pure(false);
                            }
                        });
                }
            });
        return deleted;
    };

    public Promise<Boolean> create(
        LabeledNodeWithProperties startNode,
        LabeledNodeWithProperties endNode, String location) {
        Promise<WS.Response> response =
            RelationshipService.createRelationship(
                startNode, endNode, this.type, location);
        return response.map(new SuccessFunction());
    }

    public Promise<Boolean> create(
        LabeledNodeWithProperties startNode,
        LabeledNodeWithProperties endNode, JsonNode properties,
        String location) {
        Promise<WS.Response> response =
            RelationshipService.createRelationship(
                startNode, endNode, this.type, properties, location);
        return response.map(new SuccessFunction());
    }

    public Promise<Boolean> delete(LabeledNodeWithProperties startNode,
                                   LabeledNodeWithProperties endNode,
                                   String location) {
        Promise<WS.Response> response =
            RelationshipService.deleteRelationship(
                startNode, endNode, this.type, location);
        return response.map(new SuccessFunction());
    }

    public Promise<Boolean> delete(LabeledNodeWithProperties startNode,
                                   LabeledNodeWithProperties endNode,
                                   JsonNode properties,
                                   String location) {
        Promise<WS.Response> response =
            RelationshipService.deleteRelationship(
                startNode, endNode, this.type, properties, location);
        return response.map(new SuccessFunction());
    }

    public Promise<Boolean> delete(
        LabeledNodeWithProperties startNode, String location) {
        Promise<WS.Response> response =
            RelationshipService.deleteRelationships(
                startNode, this.type, location);
        return response.map(new SuccessFunction());
    }

    public Promise<Boolean> delete(
        LabeledNodeWithProperties startNode, JsonNode properties,
        String location) {
        Promise<WS.Response> response =
            RelationshipService.deleteRelationships(
                startNode, this.type, properties, location);
        return response.map(new SuccessFunction());
    }

    @Override
    public Promise<List<JsonNode>> to(LabeledNodeWithProperties endNode) {
        Promise<WS.Response> response = RelationshipService
            .to(endNode, this.type);
        return response.map(
            new Function<WS.Response, List<JsonNode>>() {
                public List<JsonNode> apply(WS.Response response) {
                    JsonNode json = response.asJson();
                    return json.get("data").findValues("data");
                }
            });
    }

    // TypedRelManager subclasses using this will need to convert list
    // of `JsonNode`s returned by this to appropriate model objects.
    // Note that not all subclasses are going to make use of this, so
    // it doesn't make sense to force them to implement a method that
    // handles the conversion.
    public Promise<List<JsonNode>> endNodes(
        LabeledNodeWithProperties startNode, String location) {
        Promise<WS.Response> response = RelationshipService
            .endNodes(startNode, this.type, location);
        return response.map(
            new Function<WS.Response, List<JsonNode>>() {
                public List<JsonNode> apply(WS.Response response) {
                    JsonNode json = response.asJson();
                    List<JsonNode> nodes = new ArrayList<JsonNode>();
                    List<JsonNode> rows = json.findValue("data")
                        .findValues("row");
                    for (JsonNode row : rows) {
                        nodes.add(row.get(0));
                    }
                    return nodes;
                }
            });
    }

}
