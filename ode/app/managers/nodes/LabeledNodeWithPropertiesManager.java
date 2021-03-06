// LabeledNodeWithPropertiesManager.java --- Common base class for managers that deal with labeled nodes that have one or more properties.

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
import play.libs.F.Promise;


abstract class LabeledNodeWithPropertiesManager extends LabeledNodeManager {

    protected Promise<Boolean> connect(
        JsonNode startNode, JsonNode endNode, String location) {
        return Promise.pure(false);
    }

    protected Promise<Boolean> disconnect(
        JsonNode startNode, JsonNode endNode, String location) {
        return Promise.pure(false);
    }

}
