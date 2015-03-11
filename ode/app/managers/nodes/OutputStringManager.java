// OutputStringManager.java --- Manager that handles operations involving OutputString nodes.

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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import constants.NodeType;


public class OutputStringManager extends ContentNodeManager {

    public OutputStringManager() {
        this.label = NodeType.OUTPUT_STRING.toString();
    }

    // Custom functionality

    protected JsonNode toJSON(JsonNode properties) {
        ArrayNode tokens = JsonNodeFactory.instance.arrayNode();
        String content = properties.get("content").asText();
        String[] contentTokens = content.split(" ");
        for (String token: contentTokens) {
            tokens.add(token);
        }
        ((ObjectNode) properties).put("tokens", tokens);
        return properties;
    }

}
