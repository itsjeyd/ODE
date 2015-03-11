// LabeledNodeWithProperties.java --- Common base class for models representing labeled nodes that have one or more properties.

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
import com.fasterxml.jackson.databind.node.ObjectNode;
import constants.NodeType;
import play.libs.Json;


public abstract class LabeledNodeWithProperties extends LabeledNode {

    public ObjectNode jsonProperties;

    protected LabeledNodeWithProperties(NodeType label) {
        this.label = label;
        this.jsonProperties = Json.newObject();
    }

    public JsonNode getProperties() {
        return this.jsonProperties;
    }

}
