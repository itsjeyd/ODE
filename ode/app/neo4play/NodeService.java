// NodeService.java --- CRUD operations for nodes.

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

package neo4play;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F.Promise;
import play.libs.WS;


public class NodeService extends Neo4j {

    private static String buildMatchQuery(String label, JsonNode props) {
        return String.format("MATCH (n:%s) WHERE ", label)
            + buildConjunctiveConstraints("n", props);
    }

    public static Promise<WS.Response> getNodes(String label) {
        String query = String.format("MATCH (n:%s) RETURN n", label);
        return postCypherQuery(query);
    }

    public static Promise<WS.Response> getNode(
        String label, JsonNode properties) {
        String query = buildMatchQuery(label, properties) + " RETURN n";
        return postCypherQuery(query);
    }

    public static Promise<WS.Response> createNode(
        JsonNode properties, String location) {
        String query = "CREATE (n {props}) RETURN n";
        JsonNode statements = buildStatements(query, properties);
        return executeInTransaction(location, statements);
    }

    public static Promise<WS.Response> createNode(
        String label, JsonNode properties, String location) {
        String query = String
            .format("CREATE (n:%s {props}) RETURN n", label);
        JsonNode statements = buildStatements(query, properties);
        return executeInTransaction(location, statements);
    }

    public static Promise<WS.Response> updateNode(
        String label, JsonNode oldProperties, JsonNode newProperties) {
        String query = buildMatchQuery(label, oldProperties) +
            " SET n = {props}";
        JsonNode statements = buildStatements(query, newProperties);
        return executeAndCommit(statements);
    }

    public static Promise<WS.Response> updateNode(
        String label, JsonNode oldProperties, JsonNode newProperties,
        String location) {
        String query = buildMatchQuery(label, oldProperties) +
            " SET n = {props}";
        JsonNode statements = buildStatements(query, newProperties);
        return executeInTransaction(location, statements);
    }

    public static Promise<WS.Response> deleteNode(
        String label, JsonNode properties, String location) {
        String query = buildMatchQuery(label, properties) + " DELETE n";
        JsonNode statements = buildStatements(query, properties);
        return executeInTransaction(location, statements);
    }

}
