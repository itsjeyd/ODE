package neo4play;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.nodes.LabeledNodeWithProperties;
import play.libs.F.Promise;
import play.libs.Json;
import play.libs.WS;


public class RelationshipService extends Neo4j {

    private static String buildMatchQuery(
        String startNodeLabel, JsonNode startNodeProps,
        String endNodeLabel, JsonNode endNodeProps) {
        return String
            .format("MATCH (s:%s), (e:%s) WHERE %s AND %s",
                    startNodeLabel, endNodeLabel,
                    buildConjunctiveConstraints("s", startNodeProps),
                    buildConjunctiveConstraints("e", endNodeProps));
    }

    private static String buildMatchQuery(
        String startNodeLabel, JsonNode startNodeProps,
        String endNodeLabel, JsonNode endNodeProps, String type) {
        return String
            .format("MATCH (s:%s)-[r:%s]->(e:%s) WHERE %s AND %s",
                    startNodeLabel, type, endNodeLabel,
                    buildConjunctiveConstraints("s", startNodeProps),
                    buildConjunctiveConstraints("e", endNodeProps));
    }

    private static String buildMatchQuery(
        String startNodeLabel, JsonNode startNodeProps,
        String endNodeLabel, JsonNode endNodeProps, String type,
        JsonNode relProps) {
        return String
            .format("MATCH (s:%s)-[r:%s]->(e:%s) WHERE %s AND %s AND %s",
                    startNodeLabel, type, endNodeLabel,
                    buildConjunctiveConstraints("s", startNodeProps),
                    buildConjunctiveConstraints("e", endNodeProps),
                    buildConjunctiveConstraints("r", relProps));
    }

    private static JsonNode buildStatements(String query) {
        ObjectNode statement = Json.newObject();
        statement.put("statement", query);
        ArrayNode statementList = JsonNodeFactory.instance.arrayNode();
        statementList.add(statement);
        ObjectNode statements = Json.newObject();
        statements.put("statements", statementList);
        return statements;
    }

    public static Promise<WS.Response> getRelationship (
        LabeledNodeWithProperties startNode,
        LabeledNodeWithProperties endNode, String type) {
        String query = buildMatchQuery(
            startNode.getLabel(), startNode.getProperties(),
            endNode.getLabel(), endNode.getProperties(), type) + " RETURN r";
        return postCypherQuery(query);
    }

    public static Promise<WS.Response> createRelationship(
        LabeledNodeWithProperties startNode,
        LabeledNodeWithProperties endNode, String type, String location) {
        String query = buildMatchQuery(
            startNode.getLabel(), startNode.getProperties(),
            endNode.getLabel(), endNode.getProperties()) +
            String.format(" CREATE (s)-[:%s]->(e)", type);
        JsonNode statements = buildStatements(query);
        return executeInTransaction(location, statements);
    }

    public static Promise<WS.Response> createRelationship(
        LabeledNodeWithProperties startNode,
        LabeledNodeWithProperties endNode, String type, JsonNode properties,
        String location) {
        String query = buildMatchQuery(
            startNode.getLabel(), startNode.getProperties(),
            endNode.getLabel(), endNode.getProperties()) +
            String.format(" CREATE (s)-[:%s {props}]->(e)", type);
        JsonNode statements = buildStatements(query, properties);
        return executeInTransaction(location, statements);
    }

    public static Promise<WS.Response> deleteRelationship(
        LabeledNodeWithProperties startNode,
        LabeledNodeWithProperties endNode, String type, String location) {
        String query = buildMatchQuery(
            startNode.getLabel(), startNode.getProperties(),
            endNode.getLabel(), endNode.getProperties(), type) + " DELETE r";
        JsonNode statements = buildStatements(query);
        return executeInTransaction(location, statements);
    }

    public static Promise<WS.Response> deleteRelationship(
        LabeledNodeWithProperties startNode,
        LabeledNodeWithProperties endNode, String type, JsonNode properties,
        String location) {
        String query = buildMatchQuery(
            startNode.getLabel(), startNode.getProperties(),
            endNode.getLabel(), endNode.getProperties(), type, properties) +
            " DELETE r";
        JsonNode statements = buildStatements(query);
        return executeInTransaction(location, statements);
    }

    public static Promise<WS.Response> deleteRelationships(
        LabeledNodeWithProperties startNode, String type, String location) {
        String query = String.format(
            "MATCH (s:%s)-[r:%s]->() WHERE %s DELETE r",
            startNode.getLabel(), type,
            buildConjunctiveConstraints("s", startNode.getProperties()));
        JsonNode statements = buildStatements(query);
        return executeInTransaction(location, statements);
    }

    public static Promise<WS.Response> deleteRelationships(
        LabeledNodeWithProperties startNode, String type,
        JsonNode properties, String location) {
        String query = String.format(
            "MATCH (s:%s)-[r:%s]->() WHERE %s AND %s DELETE r",
            startNode.getLabel(), type,
            buildConjunctiveConstraints("s", startNode.getProperties()),
            buildConjunctiveConstraints("r", properties));
        JsonNode statements = buildStatements(query);
        return executeInTransaction(location, statements);
    }

    public static Promise<WS.Response> to(LabeledNodeWithProperties endNode) {
        String query = String.format(
            "MATCH ()-[r]->(e:%s) WHERE %s RETURN r",
            endNode.getLabel(),
            buildConjunctiveConstraints("e", endNode.getProperties()));
        return postCypherQuery(query);
    }

    public static Promise<WS.Response> to(
        LabeledNodeWithProperties endNode, String type) {
        String query = String.format(
            "MATCH ()-[r:%s]->(e:%s) WHERE %s RETURN r",
            type, endNode.getLabel(),
            buildConjunctiveConstraints("e", endNode.getProperties()));
        return postCypherQuery(query);
    }

    public static Promise<WS.Response> endNodes(
        LabeledNodeWithProperties startNode, String type) {
        String query = String.format(
            "MATCH (s:%s)-[r:%s]->(e) WHERE %s RETURN e",
            startNode.getLabel(), type,
            buildConjunctiveConstraints("s", startNode.getProperties()));
        return postCypherQuery(query);
    }

    public static Promise<WS.Response> endNodes(
        LabeledNodeWithProperties startNode, String type,
        JsonNode properties) {
        String query = String.format(
            "MATCH (s:%s)-[r:%s]->(e) WHERE %s AND %s RETURN e",
            startNode.getLabel(), type,
            buildConjunctiveConstraints("s", startNode.getProperties()),
            buildConjunctiveConstraints("r", properties));
        return postCypherQuery(query);
    }

    public static Promise<WS.Response> endNodes(
        LabeledNodeWithProperties startNode, String type, String location) {
        String query = String.format(
            "MATCH (s:%s)-[r:%s]->(e) WHERE %s RETURN e",
            startNode.getLabel(), type,
            buildConjunctiveConstraints("s", startNode.getProperties()));
        JsonNode statements = buildStatements(query);
        return executeInTransaction(location, statements);
    }

    public static Promise<WS.Response> getRelationshipVariableLength(
        LabeledNodeWithProperties startNode,
        LabeledNodeWithProperties endNode, String type,
        int minHops, int maxHops) {
        String query = String.format(
            "MATCH (s:%s)-[r:%s*%d..%d]->(e) WHERE %s AND %s RETURN r",
            startNode.getLabel(), type, minHops, maxHops,
            buildConjunctiveConstraints("s", startNode.getProperties()),
            buildConjunctiveConstraints("e", endNode.getProperties()));
        return postCypherQuery(query);
    }

}
