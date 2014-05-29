package managers.functions;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F.Function;
import play.libs.WS;


public class JsonFunction implements Function<WS.Response, JsonNode> {

    public JsonNode apply(WS.Response response) {
        return response.asJson();
    }

}
