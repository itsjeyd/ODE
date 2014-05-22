package models.nodes;

import constants.NodeType;
import managers.nodes.FooManager;
import play.libs.F.Promise;


public class Foo extends LabeledNodeWithProperties {

    public String foo;
    public static final FooManager nodes = new FooManager();

    private Foo() {
        super(NodeType.FOO);
    }

    public Foo(String foo) {
        this();
        this.foo = foo;
        this.jsonProperties.put("foo", foo);
    }

    public Promise<Boolean> create() {
        return null;
    }

    public Promise<Boolean> delete() {
        return null;
    }

}
