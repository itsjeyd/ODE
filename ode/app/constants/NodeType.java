package constants;

public enum NodeType {
    USER, FEATURE, VALUE, RULE;

    @Override
    public String toString() {
        String name = this.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }
}
