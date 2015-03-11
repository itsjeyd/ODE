// ContentNodeManager.java --- Common base class for managers that deal with nodes representing textual content.

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

package managers.nodes;

import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F.Function;
import play.libs.F.Promise;


abstract class ContentNodeManager extends LabeledNodeWithPropertiesManager {

    // READ

    @Override
    public Promise<Boolean> exists(JsonNode properties) {
        return super.exists(properties, "uuid");
    }

    // CREATE

    @Override
    public Promise<Boolean> create(
        final JsonNode properties, final String location) {
        Promise<Boolean> exists = exists(properties);
        Promise<Boolean> created = exists.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean exists) {
                    if (exists) {
                        return Promise.pure(true);
                    }
                    return ContentNodeManager.super
                        .create(properties, location);
                }
            });
        return created;
    }

}
