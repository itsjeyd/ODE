package constants;

public enum FeatureType {
    COMPLEX, ATOMIC;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
