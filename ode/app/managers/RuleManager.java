package managers;

import play.libs.F.Promise;

import models.Rule;


public class RuleManager extends LabeledNodeWithPropertiesManager {

    public static Promise<Boolean> create(Rule rule) {
        rule.jsonProperties.put("description", rule.description);
        return LabeledNodeWithPropertiesManager.create(rule);
    }

}
