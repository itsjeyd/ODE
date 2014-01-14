package managers.functions;

import play.libs.WS;
import play.mvc.Http.Status;


public class RelationshipCreatedFunction extends CreatedFunction {

    public Boolean apply(WS.Response response) {
        return response.getStatus() == Status.CREATED;
    }

}
