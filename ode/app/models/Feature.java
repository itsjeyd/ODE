package models;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.None;
import play.libs.F.Option;
import play.libs.F.Promise;
import play.libs.F.Some;
import play.libs.F.Tuple;

import constants.FeatureType;
import constants.NodeType;
import constants.RelationshipType;
import managers.FeatureManager;


public class Feature extends OntologyNode {
    protected FeatureType type;
    protected String description;
    public List<String> targets;

    private Feature() {
        this.label = NodeType.FEATURE;
        this.jsonProperties = Json.newObject();
    }

    public Feature(String name) {
        this();
        this.name = name;
        this.jsonProperties.put("name", name);
    }

    public Feature(String name, String description) {
        this(name);
        this.description = description;
    }

    public String getType() {
        return this.type.toString();
    }

    public String getDescription() {
        return this.description;
    }

    public static Promise<List<Feature>> all() {
        Promise<List<JsonNode>> json = FeatureManager.all();
        return json.map(new AllFunction());
    }

    public Promise<Feature> get() {
        Promise<JsonNode> json = FeatureManager.get(this);
        return json.map(new GetFunction());
    }

    public Promise<List<Relationship>> getOutgoingRelationships(
        RelationshipType type) {
        return TypedRelationship.getAllFrom(this, type);
    }

    public void setTargets() {
        Promise<List<JsonNode>> nodes = FeatureManager.values(this);
        Promise<List<String>> targets = nodes.map(new TargetsFunction());
        this.targets = targets.get();
    }

    public Promise<Tuple<Option<Feature>, Boolean>> updateType(
        final String newType) {
        Promise<Feature> feature = this.get();
        return feature.flatMap(
            new Function<Feature, Promise<Tuple<Option<Feature>, Boolean>>>() {
                public Promise<Tuple<Option<Feature>, Boolean>> apply(
                    Feature feature) {
                    if (feature.getType().equals(newType)) {
                        return Promise.pure(
                            new Tuple<Option<Feature>, Boolean>(
                                new Some<Feature>(feature), false));
                    }
                    Promise<Boolean> allDeleted =
                        AllowsRelationship.deleteAllFrom(feature);
                    return allDeleted.flatMap(
                        new UpdateTypeFunction(feature, newType));
                    }
                });
    }


    private static class AllFunction
        implements Function<List<JsonNode>, List<Feature>> {
        public List<Feature> apply(List<JsonNode> dataNodes) {
            List<Feature> features = new ArrayList<Feature>();
            for (JsonNode dataNode: dataNodes) {
                String name = dataNode.get("name").asText();
                String description = "";
                if (dataNode.has("description")) {
                    description = dataNode.get("description").asText();
                }
                String type = dataNode.get("type").asText();
                if (type.equals(FeatureType.COMPLEX.toString())) {
                    features.add(new ComplexFeature(name, description));
                } else if (type.equals(FeatureType.ATOMIC.toString())) {
                    features.add(new AtomicFeature(name, description));
                }
            }
            return features;
        }
    }

    private class GetFunction implements Function<JsonNode, Feature> {
        public Feature apply(JsonNode json) {
            String name = json.findValue("name").asText();
            String description = "";
            JsonNode descriptionNode = json.findValue("description");
            if (descriptionNode != null) {
                description = descriptionNode.asText();
            }
            String type = json.findValue("type").asText();
            Feature feature = null;
            if (type.equals(FeatureType.COMPLEX.toString())) {
                feature = new ComplexFeature(name, description);
            } else if (type.equals(FeatureType.ATOMIC.toString())) {
                feature = new AtomicFeature(name, description);
            }
            return feature;
        }
    }

    private class TargetsFunction
        implements Function<List<JsonNode>, List<String>> {
        public List<String> apply(List<JsonNode> nodes) {
            List<String> targets = new ArrayList<String>();
            for (JsonNode node: nodes) {
                targets.add(node.get("name").asText());
            }
            return targets;
        }
    }

    private class UpdateTypeFunction
        implements Function<Boolean, Promise<Tuple<Option<Feature>, Boolean>>> {
        private Feature feature;
        private String newType;
        public UpdateTypeFunction(Feature feature, String newType) {
            this.feature = feature;
            this.newType = newType;
        }
        public Promise<Tuple<Option<Feature>, Boolean>> apply(
            Boolean allDeleted) {
            if (allDeleted) {
                Promise<Boolean> typeUpdated = FeatureManager.updateType(
                    this.feature, this.newType);
                return typeUpdated.map(new UpdatedFunction(this.feature));
            }
            return Promise.pure(
                new Tuple<Option<Feature>, Boolean>(
                    new None<Feature>(), false));
        }
    }

    private class UpdatedFunction
        implements Function<Boolean, Tuple<Option<Feature>, Boolean>> {
        private Feature feature;
        public UpdatedFunction(Feature feature) {
            this.feature = feature;
        }
        public Tuple<Option<Feature>, Boolean> apply(Boolean updated) {
            if (updated) {
                return new Tuple<Option<Feature>, Boolean>(
                    new Some<Feature>(this.feature), true);
            }
            return new Tuple<Option<Feature>, Boolean>(
                new None<Feature>(), false);
        }
    }

}
