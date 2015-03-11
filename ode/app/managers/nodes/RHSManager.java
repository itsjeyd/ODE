// RHSManager.java --- Manager that handles operations involving RHS nodes.

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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import models.nodes.CombinationGroup;
import models.nodes.RHS;
import models.relationships.Has;
import neo4play.Neo4j;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.WS;


public class RHSManager extends CollectionNodeManager {

    public RHSManager() {
        this.label = NodeType.RHS.toString();
    }

    // READ

    public Promise<RHS> get(JsonNode properties) {
        final RHS rhs = new RHS(properties.get("uuid").asText());
        Promise<JsonNode> json = toJSON(properties);
        return json.map(
            new Function<JsonNode, RHS>() {
                public RHS apply(JsonNode json) {
                    rhs.json = json;
                    return rhs;
                }
            });
    }

    private Promise<JsonNode> toJSON(final JsonNode properties) {
        Promise<List<JsonNode>> groups = groups(properties);
        Promise<JsonNode> json = groups.map(
            new Function<List<JsonNode>, JsonNode>() {
                public JsonNode apply(List<JsonNode> groups) {
                    ArrayNode groupNodes =
                        JsonNodeFactory.instance.arrayNode();
                    groupNodes.addAll(groups);
                    ((ObjectNode) properties).put("groups", groupNodes);
                    return properties;
                }
            });
        return json;
    }

    public Promise<List<JsonNode>> groups(JsonNode properties) {
        RHS rhs = new RHS(properties.get("uuid").asText());
        Promise<List<JsonNode>> nodes = Has.relationships.endNodes(rhs);
        Promise<List<JsonNode>> groups = nodes.flatMap(
            new Function<List<JsonNode>, Promise<List<JsonNode>>>() {
                public Promise<List<JsonNode>> apply(List<JsonNode> nodes) {
                    List<Promise<? extends JsonNode>> groups =
                        new ArrayList<Promise<? extends JsonNode>>();
                    for (JsonNode node: nodes) {
                        Promise<JsonNode> group = CombinationGroup.nodes
                            .toJSON(node);
                        groups.add(group);
                    }
                    return Promise.sequence(groups);
                }
            });
        return groups;
    }

    // CREATE

    @Override
    protected Promise<Boolean> create(
        JsonNode properties, final String location) {
        final RHS rhs = new RHS(properties.get("uuid").asText());
        // 1. Create RHS
        Promise<Boolean> created = super.create(properties, location);
        // 2. Generate UUID for combination group
        final String uuid = UUID.randomUUID().toString();
        // 3. Create default combination group
        created = created.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean created) {
                    if (created) {
                        int position = 1;
                        ObjectNode props = Json.newObject();
                        props.put("uuid", uuid);
                        props.put("position", position);
                        return CombinationGroup.nodes
                            .create(props, location);
                    }
                    return Promise.pure(false);
                }
            });
        // 3. Connect RHS to combination group
        created = created.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean created) {
                    if (created) {
                        CombinationGroup group = new CombinationGroup(uuid);
                        return Has.relationships.create(rhs, group, location);
                    }
                    return Promise.pure(false);
                }
            });
        return created;
    }

    // DELETE

    protected Promise<Boolean> empty(JsonNode properties, String location) {
        RHS rhs = new RHS(properties.get("uuid").asText());
        return super.empty(rhs, location);
    }

    // Connections to other nodes

    @Override
    protected Promise<Boolean> connect(
        final JsonNode rhs, final JsonNode group, final String location) {
        Promise<Boolean> created = CombinationGroup.nodes
            .create(group, location);
        Promise<Boolean> connected = created.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean created) {
                    if (created) {
                        RHS r = new RHS(rhs.get("uuid").asText());
                        CombinationGroup g = new CombinationGroup(
                            group.get("uuid").asText(),
                            group.get("position").asInt());
                        return Has.relationships.create(r, g, location);
                    }
                    return Promise.pure(false);
                }
            });
        return connected;
    }

    @Override
    protected Promise<Boolean> disconnect(
        JsonNode rhs, final JsonNode group, final String location) {
        RHS r = new RHS(rhs.get("uuid").asText());
        CombinationGroup g = new CombinationGroup(group.get("uuid").asText());
        // 1. Disconnect RHS from group
        Promise<Boolean> disconnected = Has.relationships
            .delete(r, g, location);
        // 2. Delete group
        disconnected = disconnected.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean disconnected) {
                    if (disconnected) {
                        return CombinationGroup.nodes.delete(group, location);
                    }
                    return Promise.pure(false);
                }
            });
        return disconnected;
    }

    // Custom functionality

    protected Promise<Boolean> find(
        final JsonNode properties, JsonNode strings) {
        Promise<List<String>> stringsNotFound =
            findInOutputStrings(properties, strings);
        stringsNotFound = stringsNotFound.flatMap(
            new Function<List<String>, Promise<List<String>>>() {
                public Promise<List<String>> apply(
                    final List<String> stringsNotFound) {
                    if (stringsNotFound.isEmpty()) {
                        return Promise.pure(stringsNotFound);
                    }
                    RHS rhs = new RHS(properties.get("uuid").asText());
                    Promise<List<JsonNode>> groupNodes =
                        Has.relationships.endNodes(rhs);
                    return groupNodes.flatMap(
                        new Function<List<JsonNode>, Promise<List<String>>>() {
                            public Promise<List<String>> apply(
                                List<JsonNode> groupNodes) {
                                return findInGroupTables(
                                    groupNodes, stringsNotFound);
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
        JsonNode properties, JsonNode strings) {
        List<Promise<? extends String>> stringsNotFound =
            new ArrayList<Promise<? extends String>>();
        Iterator<JsonNode> stringIter = strings.elements();
        while (stringIter.hasNext()) {
            final JsonNode string = stringIter.next();
            Promise<Boolean> stringFound = has(properties, string);
            stringsNotFound.add(
                stringFound.map(
                    new Function<Boolean, String>() {
                        public String apply(Boolean stringFound) {
                            if (stringFound) {
                                return "";
                            }
                            return string.get("content").asText();
                        }
                    }));
        }
        return Promise.sequence(stringsNotFound).map(
            new Function<List<String>, List<String>>() {
                public List<String> apply(List<String> stringsNotFound) {
                    List<String> strings = new ArrayList<String>();
                    for (String str: stringsNotFound) {
                        if (!str.equals("")) {
                            strings.add(str);
                        }
                    }
                    return strings;
                }
            });

    }

    private Promise<Boolean> has(JsonNode properties, JsonNode string) {
        String uuid = properties.get("uuid").asText();
        String content = string.get("content").asText();
        String query = String.format(
            "MATCH p=(s:RHS)-[*2]->(t:OutputString) " +
            "WHERE s.uuid='%s' AND lower(t.content) =~ lower('.*%s.*') " +
            "RETURN p",
            uuid, content);
        Promise<WS.Response> response = Neo4j.executeCustomQuery(query);
        return response.map(
            new Function<WS.Response, Boolean>() {
                public Boolean apply(WS.Response response) {
                    JsonNode json = response.asJson();
                    return json.findValue("data").size() > 0;
                }
            });
    }

    private Promise<List<String>> findInGroupTables(
        final List<JsonNode> groups, List<String> strings) {
        if (groups.isEmpty()) {
            return Promise.pure(strings);
        }
        JsonNode group = groups.get(0);
        Promise<List<String>> stringsNotFound = CombinationGroup.nodes
            .find(group, strings);
        return stringsNotFound.flatMap(
            new Function<List<String>, Promise<List<String>>>() {
                public Promise<List<String>> apply(
                    List<String> stringsNotFound) {
                    if (stringsNotFound.isEmpty()) {
                        return Promise.pure(stringsNotFound);
                    }
                    List<JsonNode> remainingGroups =
                        groups.subList(1, groups.size());
                    return findInGroupTables(
                        remainingGroups, stringsNotFound);
                }
            });

    }

}
