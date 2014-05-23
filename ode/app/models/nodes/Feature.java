package models.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.libs.F.Function;
import play.libs.F.Promise;

import constants.FeatureType;
import constants.NodeType;
import managers.nodes.FeatureManager;
import models.relationships.AllowsRelationship;
import models.relationships.Relationship;
import models.relationships.HasFeatureRelationship;
import models.relationships.HasSubstructureRelationship;
import models.relationships.HasValueRelationship;


public class Feature extends OntologyNode {

    public static final FeatureManager nodes = new FeatureManager();

    protected FeatureType type;
    protected String description;
    public List<String> targets;

    private Feature() {
        super(NodeType.FEATURE);
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

    public static Feature of(String name, String type) {
        Feature feature = new Feature(name);
        feature.setType(type);
        return feature;
    }

    private Promise<List<Relationship>> getIncomingRelationships() {
        return Relationship.getAllTo(this);
    }

    protected Promise<List<String>> getTargets() {
        Promise<List<JsonNode>> nodes = FeatureManager.getValues(this);
        return nodes.map(new TargetsFunction());
    }

    protected Feature setType(String type) {
        if (type.equals(FeatureType.COMPLEX.toString())) {
            this.type = FeatureType.COMPLEX;
        } else {
            this.type = FeatureType.ATOMIC;
        }
        return this;
    }

    public Promise<Feature> setTargets() {
        Promise<List<String>> targets = this.getTargets();
        final Feature feature = this;
        return targets.map(
            new Function<List<String>, Feature>() {
                public Feature apply(List<String> targets) {
                    feature.targets = targets;
                    return feature;
                }
            });
    }

    public Promise<JsonNode> toJSON() {
        final ObjectNode json = Json.newObject();
        json.put("name", this.name);
        json.put("type", this.getType());
        Promise<List<String>> targets = this.getTargets();
        return targets.map(
            new Function<List<String>, JsonNode>() {
                public JsonNode apply(List<String> targets) {
                    ArrayNode targetNodes =
                        JsonNodeFactory.instance.arrayNode();
                    for (String target: targets) {
                        targetNodes.add(target);
                    }
                    json.put("targets", targetNodes);
                    return json;
                }
            });
    }

    public String getType() {
        return this.type.toString();
    }

    public String getDescription() {
        return this.description;
    }

    public Promise<Boolean> isInUse() {
        Promise<List<Relationship>> incomingRelationships =
            this.getIncomingRelationships();
        return incomingRelationships.map(
            new Function<List<Relationship>, Boolean>() {
                public Boolean apply(
                    List<Relationship> incomingRelationships) {
                    return !incomingRelationships.isEmpty();
                }
            });
    }

    public Promise<Feature> get() {
        Promise<JsonNode> json = FeatureManager.get(this);
        return json.map(new GetFunction());
    }

    public Promise<JsonNode> getValue(Rule rule, AVM avm) {
        if (this.type.equals(FeatureType.COMPLEX)) {
            return new Substructure(rule, avm, this).toJSON();
        } else {
            Promise<Value> value =
                HasValueRelationship.getEndNode(this, rule, avm);
            return value.flatMap(
                new Function<Value, Promise<JsonNode>>() {
                    public Promise<JsonNode> apply(Value value) {
                        return value.toJSON();
                    }
                });
        }
    }

    public Promise<Boolean> setValue(
        final Value newValue, final Rule rule, final AVM avm) {
        final Feature feature = this;
        Promise<Boolean> deleted = HasValueRelationship
            .delete(this, rule, avm);
        return deleted.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean deleted) {
                    if (deleted) {
                        return new HasValueRelationship(
                            feature, newValue, rule, avm).create();
                    }
                    return Promise.pure(false);
                }
            });
    }

    public Promise<Set<Rule>> getRules() {
        Promise<JsonNode> embeddingRules = FeatureManager.getRules(this);
        return embeddingRules.map(new RuleFactoryFunction());
    }

    public Promise<Set<Rule>> getRules(Value value) {
        Promise<JsonNode> embeddingRules = FeatureManager
            .getRules(this, value);
        return embeddingRules.map(new RuleFactoryFunction());
    }

    private static class RuleFactoryFunction implements
                                          Function<JsonNode, Set<Rule>> {
        public Set<Rule> apply(JsonNode json) {
            List<JsonNode> ruleNodes = json.findValue("data")
                .findValues("data");
            return Rule.makeRules(ruleNodes);
        }
    }

    public Promise<Boolean> updateType(
        final String newType) {
        Promise<Boolean> isInUse = this.isInUse();
        return isInUse.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean isInUse) {
                    if (isInUse) {
                        return Promise.pure(false);
                    }
                    Promise<Feature> feature = Feature.this.get();
                    return feature.flatMap(
                        new Function<Feature, Promise<Boolean>>() {
                            public Promise<Boolean> apply(
                                Feature feature) {
                                if (feature.getType().equals(newType)) {
                                    return Promise.pure(false);
                                }
                                Promise<Boolean> allDeleted =
                                    AllowsRelationship.deleteAllFrom(feature);
                                return allDeleted.flatMap(
                                    new UpdateTypeFunction(feature, newType));
                            }
                        });
                }
            });
    }

    public Promise<Boolean> addDefaultValue(Rule rule, AVM parent) {
        final Feature feature = this;
        if (this.type.equals(FeatureType.COMPLEX)) {
            final Substructure substructure =
                new Substructure(rule, parent, this);
            Promise<Boolean> created = substructure.create();
            return created.flatMap(
                new Function<Boolean, Promise<Boolean>>() {
                    public Promise<Boolean> apply(Boolean created) {
                        if (created) {
                            return substructure.connectTo(feature);
                        }
                        return Promise.pure(false);
                    }
                });
        } else {
            Value defaultValue = new Value("underspecified");
            return new HasValueRelationship(
                feature, defaultValue, rule, parent).create();
        }
    }

    public Promise<Boolean> remove(final Rule rule, final AVM avm) {
        final Feature feature = this;
        Promise<Boolean> valueDeleted;
        if (this.type.equals(FeatureType.COMPLEX)) {
            final Substructure substructure =
                new Substructure(rule, avm, feature);
            Promise<Boolean> emptied = substructure.empty();
            Promise<Boolean> hasRelationshipDeleted = emptied.flatMap(
                new Function<Boolean, Promise<Boolean>>() {
                    public Promise<Boolean> apply(Boolean emptied) {
                        if (emptied) {
                            return HasSubstructureRelationship
                                .delete(feature, substructure);
                        }
                        return Promise.pure(false);
                    }
                });
            valueDeleted = hasRelationshipDeleted.flatMap(
                new Function<Boolean, Promise<Boolean>>() {
                    public Promise<Boolean> apply(
                        Boolean hasRelationshipDeleted) {
                        if (hasRelationshipDeleted) {
                            return substructure.delete();
                        }
                        return Promise.pure(false);
                    }
                });
        } else {
            valueDeleted = HasValueRelationship.delete(this, rule, avm);
        }
        return valueDeleted.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(
                    Boolean hasRelationshipdeleted) {
                    if (hasRelationshipdeleted) {
                        return HasFeatureRelationship.delete(avm, feature);
                    }
                    return Promise.pure(false);
                }
            });
    }

    public Promise<Boolean> delete() {
        final Feature feature = this;
        Promise<Boolean> allDeleted =
            AllowsRelationship.deleteAllFrom(this);
        return allDeleted.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean allDeleted) {
                    if (allDeleted) {
                        return FeatureManager.delete(feature);
                    }
                    return Promise.pure(false);
                }
            });
    }


    private static class GetFunction implements Function<JsonNode, Feature> {
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
        implements Function<Boolean, Promise<Boolean>> {
        private Feature feature;
        private String newType;
        public UpdateTypeFunction(Feature feature, String newType) {
            this.feature = feature;
            this.newType = newType;
        }
        public Promise<Boolean> apply(Boolean allDeleted) {
            if (allDeleted) {
                Promise<Boolean> typeUpdated =
                    FeatureManager.updateType(this.feature, this.newType);
                final Feature f = this.feature;
                final String t = this.newType;
                return typeUpdated.flatMap(
                    new Function<Boolean, Promise<Boolean>>() {
                        public Promise<Boolean> apply(Boolean typeUpdated) {
                            if (typeUpdated) {
                                if (t.equals(FeatureType.ATOMIC.toString())) {
                                    Value underspecified =
                                        new Value("underspecified");
                                    return underspecified.connectTo(f);
                                }
                                return Promise.pure(true);
                            }
                            return Promise.pure(false);
                        }
                    });
            }
            return Promise.pure(false);
        }
    }

}
