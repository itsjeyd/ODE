package managers.nodes;

import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.F.Promise;

import models.nodes.CombinationGroup;


public class CombinationGroupManager extends
                                         LabeledNodeWithPropertiesManager {

    public CombinationGroupManager() {
        this.label = "CombinationGroup";
    }


    public static Promise<Boolean> update(CombinationGroup group,
                                          int position) {
        ObjectNode newProps = group.jsonProperties.deepCopy();
        newProps.put("position", position);
        return LabeledNodeWithPropertiesManager
            .updateProperties(group, newProps);
    }

}
