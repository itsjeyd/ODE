package neo4play;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import play.libs.Json;
import play.libs.F.Promise;
import play.libs.WS;
import utils.StringUtils;


public class Neo4j {

    private static final String ROOT_URL = "http://localhost:7474/db/data";
    private static final String TRANSACTION_URL = ROOT_URL + "/transaction";
    private static final String CONTENT_TYPE = "application/json";

    private static JsonNode defaultStatements() {
        ObjectNode statements = Json.newObject();
        statements.putArray("statements");
        return statements;
    }

    private static Promise<WS.Response> post(
        String resourceURL, JsonNode content) {
        return WS.url(resourceURL).setContentType(CONTENT_TYPE).
            post(content);
    }

    protected static String buildConjunctiveConstraints(
        String varName, JsonNode props) {
        List<String> constraints = new ArrayList<String>();
        for (Iterator<Map.Entry<String, JsonNode>> fields = props.fields();
             fields.hasNext(); ) {
            constraints.add(
                String.format("%s.%s", varName, fields.next().toString()));
        }
        return StringUtils.join(constraints, " AND ");
    }

    protected static JsonNode buildStatements(String query,
                                            JsonNode properties) {
        ObjectNode statement = Json.newObject();
        statement.put("statement", query);
        ObjectNode parameters = Json.newObject();
        parameters.put("props", properties);
        statement.put("parameters", parameters);
        ArrayNode statementList = JsonNodeFactory.instance.arrayNode();
        statementList.add(statement);
        ObjectNode statements = Json.newObject();
        statements.put("statements", statementList);
        return statements;
    }

    protected static Promise<WS.Response> postCypherQuery(String query) {
        ObjectNode content = Json.newObject();
        content.put("query", query);
        return post(ROOT_URL + "/cypher", content);
    }

    public static Promise<WS.Response> beginTransaction() {
        return post(TRANSACTION_URL, defaultStatements());
    }

    public static Promise<WS.Response> executeInTransaction(
        String location, JsonNode statements) {
        return post(location, statements);
    }

    public static Promise<WS.Response> commitTransaction(String location) {
        return post(location + "/commit", defaultStatements());
    }

    public static Promise<WS.Response> executeAndCommit(JsonNode statements) {
        return post(TRANSACTION_URL + "/commit", statements);
    }

}
