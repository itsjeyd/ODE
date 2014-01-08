package models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.WS;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.mvc.Http.Status;

import neo4play.Neo4jService;


public abstract class Model {
    private Neo4jService dbService;

    public String label;

    public ObjectNode jsonProperties;

    public abstract Promise<? extends Model> create();
    public abstract Promise<Boolean> exists();

    protected class CreatedFunction<A> implements Function<WS.Response, A> {
        private A a;
        public CreatedFunction(A a) {
            this.a = a;
        }
        public A apply(WS.Response response) {
            if (response.getStatus() == Status.OK) {
                return this.a;
            }
            return null;
        }
    }

    protected class ExistsFunction implements
                                       Function<WS.Response, Boolean> {
        public Boolean apply(WS.Response response) {
            JsonNode json = response.asJson();
            if (json.get("data").size() > 0) {
                return true;
            }
            return false;
        }
    }

    protected class DeletedFunction<A> implements Function<WS.Response, A> {
        private A a;
        public DeletedFunction(A a) {
            this.a = a;
        }
        public A apply(WS.Response response) {
            JsonNode json = response.asJson();
            if (json.get("data").size() == 0) {
                return null;
            }
            return this.a;
        }
    }

}
