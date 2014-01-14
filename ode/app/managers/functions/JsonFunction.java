package managers.functions;

import com.fasterxml.jackson.databind.JsonNode;

import play.libs.WS;
import play.libs.F.Function;


public class JsonFunction implements Function<WS.Response, JsonNode> {

    public JsonNode apply(WS.Response response) {
        return response.asJson();
    }

}
