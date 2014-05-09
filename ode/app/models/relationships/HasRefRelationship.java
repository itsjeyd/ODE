package models.relationships;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;

import constants.RelationshipType;
import models.nodes.Rule;
import models.nodes.Slot;
import managers.relationships.HasRefRelationshipManager;


public class HasRefRelationship extends TypedRelationship {

    public HasRefRelationship(Slot startNode, Rule endNode) {
        super(RelationshipType.HAS, startNode, endNode);
    }

    public Promise<Boolean> create() {
        return this.exists().flatMap(new CreateFunction());
    }

    public static Promise<List<Rule>> getEndNodes(final Slot startNode) {
        Promise<List<JsonNode>> endNodes = HasRefRelationshipManager
            .getEndNodes(startNode);
        return endNodes.map(
            new Function<List<JsonNode>, List<Rule>>() {
                public List<Rule> apply(List<JsonNode> ruleNodes) {
                    List<Rule> rules = new ArrayList<Rule>();
                    for (JsonNode ruleNode: ruleNodes) {
                        if (!ruleNode.has("content")) {
                            String name = ruleNode
                                .findValue("name").asText();
                            Rule rule = new Rule(name);
                            rules.add(rule);
                        }
                    }
                    return rules;
                }
            });
    }

    public static Promise<Boolean> delete(Slot startNode, Rule endNode) {
        return HasRefRelationshipManager.delete(startNode, endNode);
    }

    private class CreateFunction implements
                                     Function<Boolean, Promise<Boolean>> {
        public Promise<Boolean> apply(Boolean exists) {
            if (exists) {
                return Promise.pure(false);
            }
            return HasRefRelationshipManager
                .create(HasRefRelationship.this);
        }
    }

}
