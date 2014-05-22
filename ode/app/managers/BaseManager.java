package managers;

import managers.functions.SuccessFunction;
import neo4play.Neo4j;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.WS;


public abstract class BaseManager {

    public static Promise<String> beginTransaction() {
        Promise<WS.Response> response = Neo4j.beginTransaction();
        return response.map(
            new Function<WS.Response, String>() {
                public String apply(WS.Response response) {
                    return response.getHeader("Location");
                }
            });
    }

    public static Promise<Boolean> commitTransaction(String location) {
        Promise<WS.Response> response = Neo4j.commitTransaction(location);
        return response.map(new SuccessFunction());
    }

}
