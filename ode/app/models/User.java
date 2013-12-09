package models;

import models.ModelNode;


public class User extends ModelNode {
    public String username;
    public String password;

    public User(String username, String password) {
        super("User");
        this.username = username;
        this.password = password;
        this.setProperties();
    }
}
