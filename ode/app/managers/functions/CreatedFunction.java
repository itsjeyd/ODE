package managers.functions;

import play.libs.WS;
import play.libs.F.Function;


public abstract class CreatedFunction implements
                                          Function<WS.Response, Boolean> {}
