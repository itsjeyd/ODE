// BaseManager.java --- Common base class for node and relationship managers.

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
