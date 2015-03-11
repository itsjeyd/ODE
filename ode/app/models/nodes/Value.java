// Value.java --- Model class for Value nodes.

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

package models.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import constants.NodeType;
import managers.nodes.ValueManager;
import play.libs.F.Promise;


public class Value extends OntologyNode {

    public static final ValueManager nodes = new ValueManager();

    private Value() {
        super(NodeType.VALUE);
    }

    public Value(String name) {
        this();
        this.name = name;
        this.jsonProperties.put("name", name);
    }

    public Promise<JsonNode> toJSON() {
        JsonNode node = new TextNode(this.name);
        return Promise.pure(node);
    }

}
