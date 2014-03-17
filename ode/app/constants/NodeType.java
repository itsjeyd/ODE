package constants;

public enum NodeType {
    USER, FEATURE, VALUE, RULE, AVM, RHS;

    @Override
    public String toString() {
        String name = this.name();
        if (name.equals("AVM") || name.equals("RHS")) {
            return name;
        }
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
