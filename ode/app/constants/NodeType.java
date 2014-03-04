package constants;

public enum NodeType {
    USER, FEATURE, VALUE, RULE, AVM;

    @Override
    public String toString() {
        String name = this.name();
        if (name.equals("AVM")) {
            return name;
        }
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
