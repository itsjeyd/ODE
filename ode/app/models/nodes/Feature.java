// Feature.java --- Model class for Feature nodes.

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
import java.util.List;
import managers.nodes.FeatureManager;


public class Feature extends OntologyNode {

    public static final FeatureManager nodes = new FeatureManager();

    protected String description;
    protected String type;
    public List<String> targets;

    private Feature() {
        super(NodeType.FEATURE);
    }

    public Feature(String name) {
        this();
        this.name = name;
        this.jsonProperties.put("name", name);
    }

    public Feature(String name, String type) {
        this(name);
        this.type = type;
    }

    public Feature(String name, String description, String type) {
        this(name);
        this.description = description;
        this.type = type;
    }

    public Feature(
        String name, String description, String type, String uuid) {
        this(name);
        this.description = description;
        this.type = type;
        this.jsonProperties.put("uuid", uuid);
    }

    public Boolean isComplex() {
        return this.getType().equals("complex");
    }

    public Boolean isAtomic() {
        return this.getType().equals("atomic");
    }

    public String getType() {
        return this.type.toString();
    }

    public String getDescription() {
        return this.description;
    }

    public String getUUID() {
        return this.jsonProperties.get("uuid").asText();
    }

}
