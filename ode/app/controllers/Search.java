// Search.java --- Controller that is responsible for displaying search interface and handling search requests.

// Copyright (C) 2013-2015  Tim Krones

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.

// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import models.nodes.Feature;
import models.nodes.Rule;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.search;


public class Search extends Controller {

    private static Promise<Set<Rule>> getRulesMatchingFeatures(
        ArrayNode features) {
        Iterator<JsonNode> iter = features.elements();
        List<Promise<? extends Set<Rule>>> ruleSets =
            new ArrayList<Promise<? extends Set<Rule>>>();
        while (iter.hasNext()) {
            JsonNode feature = iter.next();
            String value = feature.get("value").asText();
            if (value.equals("")) {
                ruleSets.add(Feature.nodes.rules(feature));
            } else {
                ruleSets.add(Feature.nodes.rules(feature, value));
            }
        }
        return Promise.sequence(ruleSets).map(new IntersectFunction());
    }

    private static Promise<Set<Rule>> getRulesMatchingStrings(
        ArrayNode strings) {
        return Rule.nodes.matching(strings);
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

    @Security.Authenticated(Secured.class)
    public static Result search() {
        return ok(search.render());
    }

    @Security.Authenticated(Secured.class)
    @BodyParser.Of(BodyParser.Json.class)
    public static Promise<Result> doSearch() {
        final JsonNode json = request().body().asJson();
        ArrayNode features = (ArrayNode) json.findPath("features");
        final ArrayNode strings = (ArrayNode) json.findPath("strings");
        if (features.size() > 0) {
            Promise<Set<Rule>> rulesMatchingFeatures =
                getRulesMatchingFeatures(features);
            Promise<Set<Rule>> matchingRules =
                rulesMatchingFeatures.flatMap(
                    new Function<Set<Rule>, Promise<Set<Rule>>>() {
                        public Promise<Set<Rule>> apply(
                            Set<Rule> rulesMatchingFeatures) {
                            if (rulesMatchingFeatures.isEmpty() ||
                                strings.size() == 0) {
                                return Promise.pure(rulesMatchingFeatures);
                            } else {
                                return Rule.nodes.matching(
                                    rulesMatchingFeatures, strings);
                            }
                        }
                    });
            return matchingRules.map(new ResultFunction());
        } else if (strings.size() > 0) {
            Promise<Set<Rule>> rulesMatchingStrings =
                getRulesMatchingStrings(strings);
            return rulesMatchingStrings.map(new ResultFunction());
        } else {
            Set<Rule> matchingRules = new HashSet<Rule>();
            return Promise.pure(matchingRules).map(new ResultFunction());
        }
    }

    private static class ResultFunction
        implements Function<Set<Rule>, Result> {
        public Result apply(Set<Rule> matchingRules) {
            ObjectNode result = Json.newObject();
            ArrayNode ruleList = JsonNodeFactory.instance.arrayNode();
            if (matchingRules.isEmpty()) {
                return badRequest(result);
            }
            for (Rule matchingRule: matchingRules) {
                ObjectNode ruleJSON = Json.newObject();
                ruleJSON.put("name", matchingRule.name);
                ruleJSON.put("description", matchingRule.description);
                ruleList.add(ruleJSON);
            }
            result.put("matchingRules", ruleList);
            return ok(result);
        }
    }

}
