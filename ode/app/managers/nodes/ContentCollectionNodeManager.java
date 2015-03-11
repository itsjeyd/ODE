// ContentCollectionNodeManager.java --- Common base class for managers that deal with nodes representing collections that contain textual content.

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


abstract class ContentCollectionNodeManager extends CollectionNodeManager {

    // UPDATE

    public Promise<Boolean> update(
        final JsonNode collectionNode, final JsonNode oldContent,
        final JsonNode newContent) {
        Promise<String> location = beginTransaction();
        Promise<Boolean> updated = location.flatMap(
            new Function<String, Promise<Boolean>>() {
                public Promise<Boolean> apply(final String location) {
                    Promise<Boolean> updated = update(
                        collectionNode, oldContent, newContent, location);
                    return updated.flatMap(
                        new Function<Boolean, Promise<Boolean>>() {
                            public Promise<Boolean> apply(Boolean updated) {
                                if (updated) {
                                    return commitTransaction(location);
                                }
                                return Promise.pure(false);
                            }
                        });
                }
            });
        return updated;
    }

    protected Promise<Boolean> update(
        final JsonNode collectionNode, JsonNode oldContent,
        final JsonNode newContent, final String location) {
        // 1. Disconnect node from old content
        Promise<Boolean> updated =
            disconnect(collectionNode, oldContent, location);
        // 2. Connect node to new content
        updated = updated.flatMap(
            new Function<Boolean, Promise<Boolean>>() {
                public Promise<Boolean> apply(Boolean updated) {
                    if (updated) {
                        return connect(collectionNode, newContent, location);
                    }
                    return Promise.pure(false);
                }
            });
        return updated;
    }

}
