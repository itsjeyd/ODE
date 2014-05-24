package managers.nodes;

import constants.FeatureType;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.nodes.AtomicFeature;
import models.nodes.ComplexFeature;
import neo4play.RelationshipService;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import neo4play.Neo4jService;
import managers.functions.JsonFunction;
import models.functions.ExistsFunction;
import models.nodes.Feature;
import models.nodes.OntologyNode;
import models.nodes.Value;
import models.relationships.Allows;
import models.relationships.Untyped;


public class FeatureManager extends LabeledNodeWithPropertiesManager {

    public FeatureManager() {
        this.label = "Feature";
    }

    public Promise<List<Feature>> all() {
        Promise<List<JsonNode>> json = all(this.label);
        Promise<List<Feature>> features = json.map(
            new Function<List<JsonNode>, List<Feature>>() {
                public List<Feature> apply(List<JsonNode> json) {
                    List<Feature> features = new ArrayList<Feature>();
                    for (JsonNode node: json) {
                        String name = node.get("name").asText();
                        String description = "";
                        if (node.has("description")) {
                            description = node.get("description").asText();
                        }
                        String type = node.get("type").asText();
                        if (type.equals(FeatureType.COMPLEX.toString())) {
                            features.add(
                                new ComplexFeature(name, description));
                        } else if (type.equals(FeatureType.ATOMIC.toString())) {
                            features.add(
                                new AtomicFeature(name, description));
                        }
                    }
                    return features;
                }
            });
        return features.flatMap(
            new Function<List<Feature>, Promise<List<Feature>>>() {
                public Promise<List<Feature>> apply(List<Feature> features) {
                    List<Promise<? extends Feature>> all =
                        new ArrayList<Promise<? extends Feature>>();
                    for (Feature feature: features) {
                        all.add(feature.setTargets());
                    }
                    return Promise.sequence(all);
                }
            });
    }

    @Override
    protected Promise<Boolean> create(
        final JsonNode properties, final String location) {
        Promise<Boolean> created = super.create(properties, location, "name");
        if (properties.get("type").asText().equals("complex")) {
            return created;
        }
        return created.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean created) {
                    if (created) {
                        String name = properties.get("name").asText();
                        return Allows.relationships.create(
                            new Feature(name), new Value("underspecified"),
                            location);
                    }
                    return Promise.pure(false);
                }
            });
    }

    public Promise<Boolean> setType(final JsonNode properties) {
        final Feature feature = new Feature(properties.get("name").asText());
        // 1. Establish transaction
        Promise<String> location = beginTransaction();
        Promise<Boolean> updated = location.flatMap(
            new Function<String, Promise<Boolean>>() {
                public Promise<Boolean> apply(final String location) {
                    // 2. Delete all outgoing :ALLOWS relationships
                    Promise<Boolean> deleted = Allows.relationships
                        .delete(feature, location);
                    // 3. Update type
                    Promise<Boolean> updated = deleted.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean deleted) {
                                if (deleted) {
                                    ObjectNode oldProps =
                                        (ObjectNode) properties.deepCopy();
                                    oldProps.retain("name");
                                    ObjectNode newProps =
                                        (ObjectNode) properties.deepCopy();
                                    newProps.retain(
                                        "name", "description", "type");
                                    return update(
                                        oldProps, newProps, location);
                                }
                                return Promise.pure(false);
                            }
                        });
                    // 4. If new type == "atomic", connect to "underspecified"
                    if (properties.get("type").asText().equals("atomic")) {
                        updated = updated.flatMap(
                            new Function<Boolean, Promise<Boolean>>() {
                                public Promise<Boolean> apply(
                                    Boolean updated) {
                                    if (updated) {
                                        Value value =
                                            new Value("underspecified");
                                        return Allows.relationships
                                            .create(feature, value, location);
                                    }
                                    return Promise.pure(false);
                                }
                            });
                    }
                    // 5. Finalize transaction
                    updated = updated.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean updated) {
                                if (updated) {
                                    return commitTransaction(location);
                                }
                                return Promise.pure(false);
                            }
                        });
                    return updated;
                }
            });
        return updated;
    }

    protected Promise<Boolean> connect(
        JsonNode feature, final JsonNode target, final String location) {
        String fname = feature.get("name").asText();
        String tname = target.get("name").asText();
        final Feature f = new Feature(fname);
        if (feature.get("type").asText().equals("complex")) {
            return Allows.relationships
                .create(f, new Feature(tname), location);
        }
        final Value v = new Value(tname);
        Promise<Boolean> exists = Value.nodes.exists(target);
        Promise<Boolean> connected = exists.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean exists) {
                    if (exists) {
                        return Allows.relationships.create(f, v, location);
                    }
                    Promise<Boolean> created = Value.nodes
                        .create(target, location);
                    return created.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean created) {
                                if (created) {
                                    return Allows.relationships
                                        .create(f, v, location);
                                }
                                return Promise.pure(false);
                            }
                        });
                }
            });
        return connected;
    }

    protected Promise<Boolean> disconnect(
        JsonNode feature, final JsonNode target, final String location) {
        String fname = feature.get("name").asText();
        String tname = target.get("name").asText();
        Feature f = new Feature(fname);
        if (feature.get("type").asText().equals("complex")) {
            return Allows.relationships
                .delete(f, new Feature(tname), location);
        }
        Value v = new Value(tname);
        return Allows.relationships.delete(f, v, location);

    }

    public Promise<Boolean> orphaned(JsonNode properties) {
        Feature feature = new Feature(properties.get("name").asText());
        return super.orphaned(feature, Untyped.relationships);
    }


    public Promise<Boolean> has(JsonNode feature, JsonNode value) {
        Feature f = new Feature(feature.get("name").asText());
        OntologyNode v;
        String vname = value.get("name").asText();
        if (feature.get("type").asText().equals("complex")) {
            v = new Feature(vname);
        } else {
            v = new Value(vname);
        }
        Promise<WS.Response> response = RelationshipService
            .getRelationshipVariableLength(f, v, "HAS", 1, 2);
        Promise<JsonNode> json = response.map(new JsonFunction());
        return json.map(new ExistsFunction());
    }


    public static Promise<List<JsonNode>> getValues(Feature feature) {
        Promise<List<WS.Response>> responses = Neo4jService
            .getRelationshipTargets(feature.getLabel(),
                                    feature.jsonProperties,
                                    RelationshipType.ALLOWS.toString());
        return responses.map(
            new Function<List<WS.Response>, List<JsonNode>>() {
                public List<JsonNode> apply(List<WS.Response> responses) {
                    List<JsonNode> nodes = new ArrayList<JsonNode>();
                    for (WS.Response response: responses) {
                        JsonNode json = response.asJson();
                        nodes.add(json.findValue("data"));
                    }
                    return nodes;
                }
            });
    }

    public static Promise<JsonNode> getRules(Feature feature) {
        Promise<WS.Response> response = Neo4jService
            .findEmbeddingNodesAnyDepth(feature, "Rule");
        return response.map(new JsonFunction());
    }

    public static Promise<JsonNode> getRules(Feature feature, Value value) {
        String query = String.format(
            "MATCH (e:Rule)-[:LHS]->()-[*]->(s:Feature)-[r:HAS]->(v:Value) " +
            "WHERE s.name = '%s' AND v.name='%s' AND r.rule = e.uuid " +
            "RETURN e",
            feature.name, value.name);
        Promise<WS.Response> response = Neo4jService
            .executeCustomQuery(query);
        return response.map(new JsonFunction());
    }

    public static Promise<Boolean> updateType(
        Feature feature, String newType) {
        feature.jsonProperties.put(
            "description", feature.getDescription());
        ObjectNode newProps = feature.jsonProperties.deepCopy();
        newProps.put("type", newType);
        return LabeledNodeWithPropertiesManager
            .updateProperties(feature, newProps);
    }

}
