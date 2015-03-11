// LHSManager.java --- Manager that handles operations involving top-level AVM nodes.

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

package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.List;
import models.nodes.LHS;
import play.libs.F.Function;
import play.libs.F.Promise;


public class LHSManager extends AVMManager {

    // READ

    public Promise<LHS> get(JsonNode properties) {
        final LHS lhs = new LHS(properties.get("uuid").asText());
        Promise<JsonNode> json = toJSON(properties);
        return json.map(
            new Function<JsonNode, LHS>() {
                public LHS apply(JsonNode json) {
                    lhs.json = json;
                    return lhs;
                }
            });
    }

    // Custom functionality

    protected Promise<List<JsonNode>> features(JsonNode properties) {
        Promise<JsonNode> lhs = LHS.nodes.toJSON(properties);
        Promise<List<JsonNode>> features = lhs.map(
            new Function<JsonNode, List<JsonNode>>() {
                public List<JsonNode> apply(JsonNode lhs) {
                    List<JsonNode> features = new ArrayList<JsonNode>();
                    List<JsonNode> nodes = lhs.findValues("attribute");
                    for (JsonNode node: nodes) {
                        features.add(((ObjectNode) node).retain("name"));
                    }
                    return features;
                }
            });
        return features;
    }

}
