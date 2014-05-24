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
