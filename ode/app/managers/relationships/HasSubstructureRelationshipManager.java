package managers.relationships;

import java.util.UUID;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.mvc.Http.Status;

import constants.RelationshipType;
import neo4play.Neo4jService;
import models.nodes.Feature;
import models.nodes.Substructure;


public class HasSubstructureRelationshipManager extends
                                                    HasRelationshipManager {}
