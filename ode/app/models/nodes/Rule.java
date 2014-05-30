package models.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import constants.NodeType;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
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
                        new RHS(Rule.this).getGroups();
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

}
