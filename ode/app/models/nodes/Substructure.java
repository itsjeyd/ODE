// Substructure.java --- Model class for nested AVM nodes.

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

import java.util.UUID;
import managers.nodes.AVMManager;


public class Substructure extends AVM {

    public static final AVMManager nodes = new AVMManager();

    public AVM parent;
    public Feature embeddingFeature;

    protected Substructure(Rule rule, UUID uuid) {
        super(rule);
        this.jsonProperties.put("uuid", uuid.toString());
    }

    public Substructure(Rule rule, AVM parent, Feature embeddingFeature) {
        super(rule);
        this.parent = parent;
        this.embeddingFeature = embeddingFeature;
    }

    public Substructure(String uuid) {
        this.jsonProperties.put("uuid", uuid);
    }

}
