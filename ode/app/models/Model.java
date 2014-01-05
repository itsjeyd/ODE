package models;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Function;
import play.mvc.Http.Status;


public class Model {

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
