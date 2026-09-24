package com.vectorium.core.model;
public final class StageDescriptor {
    private final String name;
    private final String label;
    private final String description;
    private final boolean defaultEnabled;
    public StageDescriptor(String name, String label, String description, boolean defaultEnabled) {
        if (name==null||name.trim().isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (label==null||label.trim().isEmpty()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        if (description==null||description.trim().isEmpty()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        this.name=name;
        this.label=label;
        this.description=description;
        this.defaultEnabled=defaultEnabled;
    }
    public String getName() {
        return name;
    }
    public String getLabel() {
        return label;
    }
    public String getDescription() {
        return description;
    }
    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }
    @Override
    public boolean equals(Object other) {
        if (this==other) {
            return true;
        }
        if (other==null||getClass()!=other.getClass()) {
            return false;
        }
        StageDescriptor descriptor=(StageDescriptor)other;
        return defaultEnabled==descriptor.defaultEnabled&&name.equals(descriptor.name)&&label.equals(descriptor.label)&&description.equals(descriptor.description);
    }
    @Override
    public int hashCode() {
        int result=17;
        result=31*result+name.hashCode();
        result=31*result+label.hashCode();
        result=31*result+description.hashCode();
        result=31*result+(defaultEnabled?1:0);
        return result;
    }
    @Override
    public String toString() {
        return "StageDescriptor{name="+name+", label="+label+", description="+description+", defaultEnabled="+defaultEnabled+"}";
    }
}
