// PartManager.java --- Manager that handles operations involving Part nodes.

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
import constants.NodeType;
import java.util.ArrayList;
import java.util.List;
import models.nodes.Part;
import play.libs.F.Function;
import play.libs.F.Promise;


public class PartManager extends ContentNodeManager {

    public PartManager() {
        this.label = NodeType.PART.toString();
    }

    // READ

    public Promise<List<Part>> all() {
        Promise<List<JsonNode>> json = all(this.label);
        return json.map(
            new Function<List<JsonNode>, List<Part>>() {
                public List<Part> apply(List<JsonNode> json) {
                    List<Part> parts = new ArrayList<Part>();
                    for (JsonNode node: json) {
                        String content = node.get("content").asText();
                        parts.add(new Part(content));
                    }
                    return parts;
                }
            });
    }

}
