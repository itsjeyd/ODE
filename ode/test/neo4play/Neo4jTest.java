package neo4play;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;
import play.libs.Json;
import play.libs.WS;
import play.mvc.Http.Status;

import static org.fest.assertions.Assertions.assertThat;


public class Neo4jTest {
    private static short ASYNC_TIMEOUT = 500;

    @Test
    public void buildConjunctiveConstraintsTest() {
        ObjectNode props = Json.newObject();
        assertThat(Neo4j.buildConjunctiveConstraints("n", props))
            .isEqualTo("");
        props.put("prop1", "val1");
        assertThat(Neo4j.buildConjunctiveConstraints("n", props))
            .isEqualTo("n.prop1=\"val1\"");
        props.put("prop2", "val2");
        assertThat(Neo4j.buildConjunctiveConstraints("n", props))
            .isEqualTo("n.prop1=\"val1\" AND n.prop2=\"val2\"");
    }

    @Test
    public void buildStatementsTest() {
        // JSON node containing statements (to execute inside of a
        // transaction) should look like this:

        // {
        //     "statements": [
        //         {
        //             "statement": "CREATE (n {props}) RETURN n",
        //             "parameters": {
        //                 "props": {
        //                     "prop1": "val1",
        //                     "prop2": "val2",
        //                 }
        //             }
        //         }
        //     ]
        // }

        ObjectNode statements = Json.newObject();
        ArrayNode statementList = JsonNodeFactory.instance.arrayNode();
        ObjectNode statement = Json.newObject();
        String query = "CREATE (n {props}) RETURN n";
        ObjectNode parameters = Json.newObject();
        ObjectNode props = Json.newObject();
        props.put("prop1", "val1");
        props.put("prop2", "val2");
        parameters.put("props", props);
        statement.put("statement", query);
        statement.put("parameters", parameters);
        statementList.add(statement);
        statements.put("statements", statementList);

        assert(Neo4j.buildStatements(query, props).equals(statements));
    }

    @Test
    public void postCypherQueryTest() {
        String query = "RETURN 0";
        WS.Response response = Neo4j.postCypherQuery(query)
            .get(ASYNC_TIMEOUT);
        assertThat(response.getStatus()).isEqualTo(Status.OK);
    }

}
