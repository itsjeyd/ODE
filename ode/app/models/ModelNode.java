package models;

import java.util.HashMap;

import java.lang.reflect.Field;

import models.PropertyNode;


public class ModelNode extends PropertyNode {

    public ModelNode(String label) {
        super(label);
        this.properties = new HashMap<String, String>();
    }

    protected void setProperties() {
        for (Field property: getClass().getDeclaredFields()) {
            String name = property.getName();
            String value;
            try {
                value = property.get(this).toString();
            }
            catch (IllegalAccessException e) {
                continue;
            }
            this.properties.put(name, value);
        }
    }
}
