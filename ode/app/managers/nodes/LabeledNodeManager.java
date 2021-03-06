// LabeledNodeManager.java --- Common base class for managers that deal with labeled nodes.

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
import java.util.List;
import managers.functions.SuccessFunction;
import managers.relationships.RelManager;
import models.nodes.LabeledNodeWithProperties;
import neo4play.NodeService;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;


abstract class LabeledNodeManager extends NodeManager {

    protected String label;

    // READ

    public Promise<Boolean> exists(JsonNode properties) {
        return exists(this.label, properties);
    }

    protected Promise<Boolean> exists(JsonNode properties, String idField) {
        final ObjectNode props = (ObjectNode) properties.deepCopy();
        return exists(this.label, props.retain(idField));
    }

    // CREATE

    protected Promise<Boolean> create(JsonNode properties, String location) {
        Promise<WS.Response> response =
            NodeService.createNode(this.label, properties, location);
        return response.map(new SuccessFunction());
    }

    protected Promise<Boolean> create(
        final JsonNode properties, final String location, String idField) {
        Promise<Boolean> exists = exists(properties, idField);
        Promise<Boolean> created = exists.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean exists) {
                    if (exists) {
                        return Promise.pure(false);
                    }
                    Promise<WS.Response> response = NodeService.createNode(
                        LabeledNodeManager.this.label, properties, location);
                    return response.map(new SuccessFunction());
                }
            });
        return created;
    }

    // UPDATE

    public Promise<Boolean> update(
        JsonNode oldProperties, JsonNode newProperties) {
        return update(this.label, oldProperties, newProperties);
    }

    public Promise<Boolean> update(
        JsonNode oldProperties, JsonNode newProperties, String location) {
        return update(this.label, oldProperties, newProperties, location);
    }

    // DELETE

    protected Promise<Boolean> delete(JsonNode properties, String location) {
        Promise<WS.Response> response =
            NodeService.deleteNode(this.label, properties, location);
        return response.map(new SuccessFunction());
    }

    // Connections to other nodes

    protected Promise<Boolean> orphaned(
        LabeledNodeWithProperties node, RelManager relManager) {
        Promise<List<JsonNode>> incomingRelationships =
            relManager.to(node);
        return incomingRelationships.map(
            new Function<List<JsonNode>, Boolean>() {
                public Boolean apply(List<JsonNode> incomingRelationships) {
                    return incomingRelationships.size() == 0;
                }
            });
    }

}
