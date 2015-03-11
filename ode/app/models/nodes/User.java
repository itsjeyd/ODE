// User.java --- Model class for User nodes.

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

package models.nodes;

import constants.NodeType;
import managers.nodes.UserManager;


public class User extends LabeledNodeWithProperties {

    public static final UserManager nodes = new UserManager();

    public String username;
    public String password;

    private User() {
        super(NodeType.USER);
    }

}
