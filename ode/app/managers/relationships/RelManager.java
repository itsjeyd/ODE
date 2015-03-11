// RelManager.java --- Common base class for relationship managers.

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

package managers.relationships;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import managers.BaseManager;
import models.nodes.LabeledNodeWithProperties;
import neo4play.RelationshipService;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;


public abstract class RelManager extends BaseManager {

    public Promise<List<JsonNode>> to(LabeledNodeWithProperties endNode) {
        Promise<WS.Response> response = RelationshipService.to(endNode);
        return response.map(
            new Function<WS.Response, List<JsonNode>>() {
                public List<JsonNode> apply(WS.Response response) {
                    JsonNode json = response.asJson();
                    return json.get("data").findValues("data");
                }
            });
    }

}
