package managers.functions;

import play.libs.WS;
import play.libs.F.Function;


public abstract class BooleanFunction implements
                                          Function<WS.Response, Boolean> {}
