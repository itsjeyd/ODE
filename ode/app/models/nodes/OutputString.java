// OutputString.java --- Model class for OutputString nodes.

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
import managers.nodes.OutputStringManager;


public class OutputString extends LabeledNodeWithProperties {

    public static final OutputStringManager nodes =
        new OutputStringManager();

    private String content;

    private OutputString() {
        super(NodeType.OUTPUT_STRING);
    }

    public OutputString(String uuid) {
        this();
        this.jsonProperties.put("uuid", uuid);
    }

    public OutputString(String uuid, String content) {
        this(uuid);
        this.content = content;
        this.jsonProperties.put("content", content);
    }

}
