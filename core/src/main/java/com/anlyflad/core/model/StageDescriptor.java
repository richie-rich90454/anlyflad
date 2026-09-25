package com.anlyflad.core.model;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
public final class StageDescriptor {
    private final String name;
    private final String label;
    private final String description;
    private final boolean defaultEnabled;
    private final List<ParamSpec> parameters;
    public StageDescriptor(String name, String label, String description, boolean defaultEnabled) {
        this(name, label, description, defaultEnabled, Collections.<ParamSpec>emptyList());
    }
    public StageDescriptor(String name, String label, String description, boolean defaultEnabled, List<ParamSpec> parameters) {
        if (name==null||name.trim().isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (label==null||label.trim().isEmpty()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        if (description==null||description.trim().isEmpty()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        if (parameters==null) {
            throw new IllegalArgumentException("parameters must not be null");
        }
        List<ParamSpec> copiedParameters=new ArrayList<ParamSpec>(parameters.size());
        for (int index=0;index<parameters.size();index++) {
            ParamSpec parameter=parameters.get(index);
            if (parameter==null) {
                throw new IllegalArgumentException("parameters must not contain null");
            }
            copiedParameters.add(parameter);
        }
        this.name=name;
        this.label=label;
        this.description=description;
        this.defaultEnabled=defaultEnabled;
        this.parameters=Collections.unmodifiableList(copiedParameters);
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
    public List<ParamSpec> getParameters() {
        return parameters;
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
        return defaultEnabled==descriptor.defaultEnabled&&name.equals(descriptor.name)&&label.equals(descriptor.label)&&description.equals(descriptor.description)&&parameters.equals(descriptor.parameters);
    }
    @Override
    public int hashCode() {
        int result=17;
        result=31*result+name.hashCode();
        result=31*result+label.hashCode();
        result=31*result+description.hashCode();
        result=31*result+(defaultEnabled?1:0);
        result=31*result+parameters.hashCode();
        return result;
    }
    @Override
    public String toString() {
        return "StageDescriptor{name="+name+", label="+label+", description="+description+", defaultEnabled="+defaultEnabled+"}";
    }
}
