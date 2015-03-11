// Slot.java --- Model class for Slot nodes.

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
import managers.nodes.SlotManager;


public class Slot extends LabeledNodeWithProperties {

    public static final SlotManager nodes = new SlotManager();

    private Slot() {
        super(NodeType.SLOT);
    }

    public Slot(String uuid) {
        this();
        this.jsonProperties.put("uuid", uuid);
    }

    public Slot(String uuid, int position) {
        this(uuid);
        this.jsonProperties.put("position", position);
    }

}
