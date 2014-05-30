package models.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import constants.NodeType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import managers.nodes.RuleManager;
import models.nodes.RHS;
import play.libs.F.Function;
import play.libs.F.Promise;


public class Rule extends UUIDNode {

    public static final RuleManager nodes = new RuleManager();

    public String name;
    public String description;
    public LHS lhs;
    public RHS rhs;
    public String uuid;

    private Rule() {
        super(NodeType.RULE);
    }

    public Rule(String name) {
        this();
        this.name = name;
        this.jsonProperties.put("name", name);
    }

    public Rule(String name, String description) {
        this(name);
        this.description = description;
    }

    public Rule(String name, String description, String uuid) {
        this(name, description);
        this.uuid = uuid;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Rule)) {
            return false;
        }
        Rule that = (Rule) o;
        return this.name.equals(that.name);
    }

    @Override
    public int hashCode() {
        int result = 17;
        int c = this.name == null ? 0 : this.name.hashCode();
        result = 31 * result + c;
        return result;
    }

    public void setUUID(String uuid) {
        this.jsonProperties.put("uuid", uuid);
    }

    public Promise<UUID> getUUID() {
        return RuleManager.getUUID(this);
    }

    public Promise<List<Rule>> getSimilarRules() {
        LHS lhs = new LHS(this);
        Promise<List<Feature>> features = lhs.getAllFeatures();
        Promise<Set<Rule>> similarRules = features.flatMap(
            new Function<List<Feature>, Promise<Set<Rule>>>() {
                public Promise<Set<Rule>> apply(List<Feature> features) {
                    List<Promise<? extends Set<Rule>>> ruleSets =
                        new ArrayList<Promise<? extends Set<Rule>>>();
                    for (Feature feature: features) {
                        JsonNode properties = feature.getProperties();
                        ruleSets.add(Feature.nodes.rules(properties));
                    }
                    return Promise.sequence(ruleSets)
                        .map(new IntersectFunction());
                }
            });
        return similarRules.map(
            new Function<Set<Rule>, List<Rule>>() {
                public List<Rule> apply(Set<Rule> similarRules) {
                    List<Rule> rules = new ArrayList<Rule>();
                    rules.addAll(similarRules);
                    return rules;
                }
            });
    }

    private static class IntersectFunction
        implements Function<List<Set<Rule>>, Set<Rule>> {
        public Set<Rule> apply(List<Set<Rule>> ruleSets) {
            Set<Rule> rules = new HashSet<Rule>();
            boolean firstSet = true;
            for (Set<Rule> ruleSet: ruleSets) {
                if (firstSet) {
                    rules.addAll(ruleSet);
                    firstSet = false;
                } else {
                    rules.retainAll(ruleSet);
                }
            }
            return rules;
        }
    }

}
