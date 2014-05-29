package models.relationships;

import com.fasterxml.jackson.databind.JsonNode;
import constants.RelationshipType;
import java.util.ArrayList;
import java.util.List;
import managers.relationships.HasRefRelationshipManager;
import models.nodes.Rule;
import models.nodes.Slot;
import play.libs.F.Function;
import play.libs.F.Promise;


public class HasRefRelationship extends TypedRelationship {

    public HasRefRelationship(Slot startNode, Rule endNode) {
        super(RelationshipType.HAS, startNode, endNode);
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

}
