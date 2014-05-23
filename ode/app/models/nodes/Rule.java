package models.nodes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.F.Tuple;

import constants.NodeType;
import managers.nodes.RuleManager;
import models.nodes.RHS;
import models.relationships.HasRefRelationship;
import models.relationships.LHSRelationship;
import models.relationships.RHSRelationship;


public class Rule extends UUIDNode {

    public static final RuleManager nodes = new RuleManager();

    public String name;
    public String description;
    public LHS lhs;
    public RHS rhs;

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

    public Promise<Boolean> isOrphan() {
        return RuleManager.isOrphan(this);
    }

    public Promise<UUID> getUUID() {
        return RuleManager.getUUID(this);
    }

    public static Set<Rule> makeRules(List<JsonNode> ruleNodes) {
        Set<Rule> rules = new HashSet<Rule>();
        for (JsonNode ruleNode: ruleNodes) {
            String name = ruleNode.findValue("name").asText();
            String description = ruleNode.findValue("description").asText();
            rules.add(new Rule(name, description));
        }
        return rules;
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
                        ruleSets.add(feature.getRules());
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

    public static Promise<Set<Rule>> findMatching(final JsonNode strings) {
        Promise<List<Rule>> ruleList = Rule.nodes.all();
        Promise<Set<Rule>> rules = ruleList.map(
            new Function<List<Rule>, Set<Rule>>() {
                public Set<Rule> apply(List<Rule> ruleList) {
                    Set<Rule> rules = new HashSet<Rule>();
                    rules.addAll(ruleList);
                    return rules;
                }
            });
        return rules.flatMap(
            new Function<Set<Rule>, Promise<Set<Rule>>>() {
                public Promise<Set<Rule>> apply(Set<Rule> ruleNames) {
                    return Rule.findMatching(ruleNames, strings);
                }
            });
    }

    public static Promise<Set<Rule>> findMatching(
        Set<Rule> rules, JsonNode strings) {
        List<Promise<? extends Rule>> matchingRules =
            new ArrayList<Promise<? extends Rule>>();
        for (final Rule rule: rules) {
            Promise<Boolean> hasStrings = rule.has(strings);
            Promise<Rule> matchingRule = hasStrings.map(
                new Function<Boolean, Rule>() {
                    public Rule apply(Boolean hasStrings) {
                        if (hasStrings) {
                            return rule;
                        }
                        return null;
                    }
                });
            matchingRules.add(matchingRule);
        }
        return Promise.sequence(matchingRules).map(
            new Function<List<Rule>, Set<Rule>>() {
                public Set<Rule> apply(List<Rule> matchingRules) {
                    Set<Rule> result = new HashSet<Rule>();
                    for (Rule matchingRule: matchingRules) {
                        if (matchingRule != null) {
                            result.add(matchingRule);
                        }
                    }
                    return result;
                }
            });
    }

    private Promise<Boolean> has(final JsonNode searchStrings) {
        Promise<List<String>> stringsNotFound =
            this.findInOutputStrings(searchStrings);

        stringsNotFound = stringsNotFound.flatMap(
            new Function<List<String>, Promise<List<String>>>() {
                public Promise<List<String>> apply(
                    final List<String> stringsNotFound) {
                    if (stringsNotFound.isEmpty()) {
                        return Promise.pure(stringsNotFound);
                    }
                    Promise<List<CombinationGroup>> groups =
                        RHS.of(Rule.this).getGroups();
                    return groups.flatMap(
                        new Function<List<CombinationGroup>,
                                     Promise<List<String>>>() {
                            public Promise<List<String>> apply(
                                List<CombinationGroup> groups) {
                                return Rule.this.findInGroupTables(
                                    stringsNotFound, groups);
                            }
                        });
                }
            });

        return stringsNotFound.map(
            new Function<List<String>, Boolean>() {
                public Boolean apply(List<String> stringsNotFound) {
                    if (stringsNotFound.isEmpty()) {
                        return true;
                    }
                    return false;
                }
            });
    }

    private Promise<List<String>> findInOutputStrings(
        JsonNode searchStrings) {
        List<Promise<? extends String>> stringsNotFound =
            new ArrayList<Promise<? extends String>>();
        Iterator<JsonNode> strings = searchStrings.elements();
        while (strings.hasNext()) {
            JsonNode stringJSON = strings.next();
            final String string = stringJSON.get("content").textValue();
            Promise<Boolean> stringFound = RuleManager.has(this, string);
            stringsNotFound.add(
                stringFound.map(
                    new Function<Boolean, String>() {
                        public String apply(Boolean stringFound) {
                            if (stringFound) {
                                return "";
                            }
                            return string;
                        }
                    }));
        }
        return Promise.sequence(stringsNotFound).map(
            new Function<List<String>, List<String>>() {
                public List<String> apply(List<String> stringsNotFound) {
                    List<String> foo = new ArrayList<String>();
                    for (String str: stringsNotFound) {
                        if (!str.equals("")) {
                            foo.add(str);
                        }
                    }
                    return foo;
                }
            });
    }

    private Promise<List<String>> findInGroupTables(
        List<String> searchStrings, final List<CombinationGroup> groups) {
        if (groups.isEmpty()) {
            return Promise.pure(searchStrings);
        }
        CombinationGroup group = groups.get(0);
        Promise<List<String>> stringsNotFound = group
            .findStrings(searchStrings);
        return stringsNotFound.flatMap(
            new Function<List<String>, Promise<List<String>>>() {
                public Promise<List<String>> apply(
                    List<String> stringsNotFound) {
                    if (stringsNotFound.isEmpty()) {
                        return Promise.pure(stringsNotFound);
                    }
                    return Rule.this.findInGroupTables(
                        stringsNotFound, groups.subList(1, groups.size()));
                }
            });
    }

    public Promise<Rule> get() {
        Promise<JsonNode> json = RuleManager.get(this);
        return json.flatMap(new GetFunction());
    }

    public Promise<Boolean> create() {
        Promise<Boolean> created = this.exists()
            .flatMap(new CreateFunction(this));
        Promise<Boolean> lhsCreated = created
            .flatMap(new CreateLHSFunction(this));
        return lhsCreated.flatMap(new CreateRHSFunction(this));
    }

    public Promise<Boolean> connectTo(final Slot slot) {
        return this.exists().flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean exists) {
                    if (exists) {
                        return new HasRefRelationship(
                            slot, Rule.this).create();
                    }
                    return Promise.pure(false);
                }
            });
    }

    public Promise<Boolean> removeFrom(Slot slot) {
        return HasRefRelationship.delete(slot, this);
    }

    public Promise<Boolean> updateName(final String newName) {
        Promise<Rule> rule = this.get();
        return rule.flatMap(
            new Function<Rule, Promise<Boolean>>() {
                public Promise<Boolean> apply(Rule rule) {
                    return RuleManager.updateName(rule, newName);
                }
            });
    }

    public Promise<Boolean> updateDescription(final String newDescription) {
        Promise<Rule> rule = this.get();
        return rule.flatMap(
            new Function<Rule, Promise<Boolean>>() {
                public Promise<Boolean> apply(Rule rule) {
                    return RuleManager.updateDescription(
                        rule, newDescription);
                }
            });
    }

    public Promise<Boolean> updateLHS(Feature feature) {
        LHS lhs = new LHS(this);
        return lhs.add(feature);
    }

    public Promise<Boolean> addGroup(CombinationGroup group) {
        return RHS.of(this).add(group);
    }

    public Promise<Boolean> removeGroup(CombinationGroup group) {
        return RHS.of(this).remove(group);
    }

    public Promise<Boolean> deleteIfOrphaned() {
        return this.isOrphan().flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean isOrphan) {
                    if (isOrphan) {
                        return Rule.this.delete();
                    }
                    return Promise.pure(false);
                }
            });
    }

    public Promise<Boolean> delete() {
        final Rule rule = this;
        final LHS lhs = new LHS(rule);
        final RHS rhs = new RHS(rule);
        Promise<Boolean> emptied = lhs.empty();
        Promise<Boolean> lhsRelationshipDeleted = emptied.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean emptied) {
                    if (emptied) {
                        return LHSRelationship.delete(rule, lhs);
                    }
                    return Promise.pure(false);
                }
            });
        Promise<Boolean> lhsDeleted = lhsRelationshipDeleted.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(
                    Boolean lhsRelationshipDeleted) {
                    if (lhsRelationshipDeleted) {
                        return lhs.delete();
                    }
                    return Promise.pure(false);
                }
            });
        Promise<Boolean> rhsRelationshipDeleted = lhsDeleted.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean lhsDeleted) {
                    if (lhsDeleted) {
                        return RHSRelationship.delete(rule, rhs);
                    }
                    return Promise.pure(false);
                }
            });
        Promise<Boolean> rhsDeleted = rhsRelationshipDeleted.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(
                    Boolean rhsRelationshipDeleted) {
                    if (rhsRelationshipDeleted) {
                        return rhs.delete();
                    }
                    return Promise.pure(false);
                }
            });
        return rhsDeleted.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean rhsDeleted) {
                    if (rhsDeleted) {
                        return RuleManager.delete(rule);
                    }
                    return Promise.pure(false);
                }
            });
    }

    private static class GetFunction implements
                                         Function<JsonNode, Promise<Rule>> {
        public Promise<Rule> apply(JsonNode json) {
            String name = json.findValue("name").asText();
            String description = json.findValue("description").asText();
            String uuid = json.findValue("uuid").asText();
            final Rule rule = new Rule(name, description);
            rule.jsonProperties.put("uuid", uuid);
            Promise<LHS> lhs = new LHS(rule).get();
            Promise<RHS> rhs = new RHS(rule).get();
            return lhs.zip(rhs).map(
                new Function<Tuple<LHS, RHS>, Rule>() {
                    public Rule apply(Tuple<LHS, RHS> components) {
                        rule.lhs = components._1;
                        rule.rhs = components._2;
                        return rule;
                    }
                });
        }
    }

    private class CreateFunction implements
                                     Function<Boolean, Promise<Boolean>> {
        private Rule rule;
        public CreateFunction(Rule rule) {
            this.rule = rule;
        }
        public Promise<Boolean> apply(Boolean exists) {
            if (exists) {
                return Promise.pure(false);
            }
            this.rule.jsonProperties
                .put("uuid", UUID.randomUUID().toString());
            return RuleManager.create(rule);
        }
    }

    private class CreateLHSFunction implements
                                        Function<Boolean, Promise<Boolean>> {

        private Rule rule;
        public CreateLHSFunction(Rule rule) {
            this.rule = rule;
        }
        public Promise<Boolean> apply(Boolean created) {
            if (!created) {
                return Promise.pure(false);
            }
            final LHS lhs = new LHS(this.rule);
            Promise<Boolean> lhsCreated = lhs.create();
            final Rule rule = this.rule;
            return lhsCreated.flatMap(
                new Function<Boolean, Promise<Boolean>>() {
                    public Promise<Boolean> apply(Boolean created) {
                        if (created) {
                            return lhs.connectTo(rule);
                        }
                        return Promise.pure(false);
                    }
                });
        }
    }

    private class CreateRHSFunction implements
                                        Function<Boolean, Promise<Boolean>> {

        private Rule rule;
        public CreateRHSFunction(Rule rule) {
            this.rule = rule;
        }
        public Promise<Boolean> apply(Boolean lhsCreated) {
            if (!lhsCreated) {
                return Promise.pure(false);
            }
            final RHS rhs = new RHS(this.rule);
            Promise<Boolean> rhsCreated = rhs.create();
            final Rule rule = this.rule;
            return rhsCreated.flatMap(
                new Function<Boolean, Promise<Boolean>>() {
                    public Promise<Boolean> apply(Boolean created) {
                        if (created) {
                            return rhs.connectTo(rule);
                        }
                        return Promise.pure(false);
                    }
                });
        }
    }

}
