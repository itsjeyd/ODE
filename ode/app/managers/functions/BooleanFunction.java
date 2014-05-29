package managers.functions;

import play.libs.F.Function;
import play.libs.WS;


public abstract class BooleanFunction implements
                                          Function<WS.Response, Boolean> {}
