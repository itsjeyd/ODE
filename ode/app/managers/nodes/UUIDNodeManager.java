package managers.nodes;

import java.util.UUID;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;

import neo4play.Neo4jService;
import managers.functions.NodeDeletedFunction;
import models.nodes.UUIDNode;


public class UUIDNodeManager extends LabeledNodeWithPropertiesManager {}
