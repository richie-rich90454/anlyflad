package com.vectorium.core.model;
import java.util.Objects;
public final class ParamSpec {
    private final String name;
    private final String label;
    private final String description;
    private final ParamType type;
    private final Object defaultValue;
    private final Double min;
    private final Double max;
    public ParamSpec(String name, String label, String description, ParamType type, Object defaultValue, Double min, Double max) {
        if (name==null||name.trim().isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (label==null||label.trim().isEmpty()) {
            throw new IllegalArgumentException("label must not be blank");
        }
        if (description==null||description.trim().isEmpty()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        if (type==null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (defaultValue==null) {
            throw new IllegalArgumentException("defaultValue must not be null");
        }
        validateDefaultValue(type, defaultValue);
        validateRange(type, defaultValue, min, max);
        this.name=name;
        this.label=label;
        this.description=description;
        this.type=type;
        this.defaultValue=defaultValue;
        this.min=min;
        this.max=max;
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
    public ParamType getType() {
        return type;
    }
    public Object getDefaultValue() {
        return defaultValue;
    }
    public Double getMin() {
        return min;
    }
    public Double getMax() {
        return max;
    }
    @Override
    public boolean equals(Object other) {
        if (this==other) {
            return true;
        }
        if (other==null||getClass()!=other.getClass()) {
            return false;
        }
        ParamSpec spec=(ParamSpec)other;
        return name.equals(spec.name)&&label.equals(spec.label)&&description.equals(spec.description)&&type==spec.type&&defaultValue.equals(spec.defaultValue)&&Objects.equals(min, spec.min)&&Objects.equals(max, spec.max);
    }
    @Override
    public int hashCode() {
        int result=17;
        result=31*result+name.hashCode();
        result=31*result+label.hashCode();
        result=31*result+description.hashCode();
        result=31*result+type.hashCode();
        result=31*result+defaultValue.hashCode();
        result=31*result+Objects.hashCode(min);
        result=31*result+Objects.hashCode(max);
        return result;
    }
    @Override
    public String toString() {
        return "ParamSpec{name="+name+", label="+label+", description="+description+", type="+type+", defaultValue="+defaultValue+", min="+min+", max="+max+"}";
    }
    private static void validateDefaultValue(ParamType type, Object defaultValue) {
        if (type==ParamType.BOOLEAN&&!(defaultValue instanceof Boolean)) {
            throw new IllegalArgumentException("defaultValue must be Boolean for BOOLEAN");
        }
        if (type==ParamType.INTEGER&&!(defaultValue instanceof Integer)) {
            throw new IllegalArgumentException("defaultValue must be Integer for INTEGER");
        }
        if (type==ParamType.DOUBLE&&!(defaultValue instanceof Double)) {
            throw new IllegalArgumentException("defaultValue must be Double for DOUBLE");
        }
        if (type==ParamType.STRING&&!(defaultValue instanceof String)) {
            throw new IllegalArgumentException("defaultValue must be String for STRING");
        }
        if (type==ParamType.DOUBLE&&!Double.isFinite((Double)defaultValue)) {
            throw new IllegalArgumentException("defaultValue must be finite for DOUBLE");
        }
    }
    private static void validateRange(ParamType type, Object defaultValue, Double min, Double max) {
        if (!type.supportsRange()&&(min!=null||max!=null)) {
            throw new IllegalArgumentException("min and max are only valid for numeric types");
        }
        if (min!=null&&!Double.isFinite(min)) {
            throw new IllegalArgumentException("min must be finite");
        }
        if (max!=null&&!Double.isFinite(max)) {
            throw new IllegalArgumentException("max must be finite");
        }
        if (min!=null&&max!=null&&min>max) {
            throw new IllegalArgumentException("min must not exceed max");
        }
        if (type.supportsRange()) {
            double numericDefault=numericValue(type, defaultValue);
            if (min!=null&&numericDefault<min) {
                throw new IllegalArgumentException("defaultValue must not be less than min");
            }
            if (max!=null&&numericDefault>max) {
                throw new IllegalArgumentException("defaultValue must not exceed max");
            }
        }
    }
    private static double numericValue(ParamType type, Object defaultValue) {
        if (type==ParamType.INTEGER) {
            return ((Integer)defaultValue).doubleValue();
        }
        return (Double)defaultValue;
    }
}
