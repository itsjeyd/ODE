// SuccessFunction.java --- Function class that maps a WS.Response to true if request was successful (false otherwise).

// Copyright (C) 2013-2015  Tim Krones

// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.

// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.

package managers.functions;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F.Function;
import play.libs.WS;


public class SuccessFunction implements Function<WS.Response, Boolean> {

    public Boolean apply(WS.Response response) {
        JsonNode json = response.asJson();
        return json.get("errors").size() == 0;
    }

}
