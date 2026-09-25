package com.vectorium.core.stage;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
public final class PipelineConfig {
    private final boolean clean;
    private final String presetName;
    private final RasterMode rasterMode;
    private final Map<String, String> overrides;
    public PipelineConfig(boolean clean, String presetName, Map<String, String> overrides) {
        this(clean, presetName, RasterMode.COLOR, overrides);
    }
    public PipelineConfig(boolean clean, String presetName, RasterMode rasterMode, Map<String, String> overrides) {
        if (presetName==null||presetName.trim().isEmpty()) {
            throw new IllegalArgumentException("presetName must not be blank");
        }
        if (rasterMode==null) {
            throw new IllegalArgumentException("rasterMode must not be null");
        }
        if (overrides==null) {
            throw new IllegalArgumentException("overrides must not be null");
        }
        TreeMap<String, String> copiedOverrides=new TreeMap<String, String>();
        Set<String> keys=overrides.keySet();
        for (String key : keys) {
            validateKey(key);
            String value=overrides.get(key);
            if (value==null) {
                throw new IllegalArgumentException("override values must not be null");
            }
            copiedOverrides.put(key, value);
        }
        this.clean=clean;
        this.presetName=presetName;
        this.rasterMode=rasterMode;
        this.overrides=Collections.unmodifiableMap(copiedOverrides);
    }
    public static PipelineConfig defaults() {
        return new PipelineConfig(true, "default", RasterMode.COLOR, Collections.<String, String>emptyMap());
    }
    public boolean isClean() {
        return clean;
    }
    public String getPresetName() {
        return presetName;
    }
    public RasterMode getRasterMode() {
        return rasterMode;
    }
    public Map<String, String> getOverrides() {
        return overrides;
    }
    public PipelineConfig withClean(boolean clean) {
        return new PipelineConfig(clean, presetName, rasterMode, overrides);
    }
    public PipelineConfig withPreset(String presetName) {
        return new PipelineConfig(clean, presetName, rasterMode, overrides);
    }
    public PipelineConfig withRasterMode(RasterMode rasterMode) {
        return new PipelineConfig(clean, presetName, rasterMode, overrides);
    }
    public PipelineConfig withStageValue(String stageName, String parameterName, String value) {
        validateName(stageName, "stageName");
        validateName(parameterName, "parameterName");
        if (value==null) {
            throw new IllegalArgumentException("value must not be null");
        }
        TreeMap<String, String> updated=new TreeMap<String, String>(overrides);
        updated.put(qualifiedKey(stageName, parameterName), value);
        return new PipelineConfig(clean, presetName, rasterMode, updated);
    }
    public PipelineConfig withStageDisabled(String stageName) {
        return withStageValue(stageName, "enabled", "false");
    }
    public PipelineConfig withoutStage(String stageName) {
        validateName(stageName, "stageName");
        TreeMap<String, String> updated=new TreeMap<String, String>(overrides);
        String prefix=stageName+".";
        updated.subMap(prefix, prefix+"\uffff").clear();
        return new PipelineConfig(clean, presetName, rasterMode, updated);
    }
    public boolean isStageEnabled(String stageName) {
        return getBoolean(stageName, "enabled", true);
    }
    public boolean getBoolean(String stageName, String parameterName, boolean defaultValue) {
        String value=getString(stageName, parameterName, null);
        if (value==null) {
            return defaultValue;
        }
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        throw new IllegalArgumentException("stage parameter must be true or false: "+qualifiedKey(stageName, parameterName));
    }
    public int getInteger(String stageName, String parameterName, int defaultValue) {
        String value=getString(stageName, parameterName, null);
        if (value==null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("stage parameter must be an integer: "+qualifiedKey(stageName, parameterName), exception);
        }
    }
    public double getDouble(String stageName, String parameterName, double defaultValue) {
        String value=getString(stageName, parameterName, null);
        if (value==null) {
            return defaultValue;
        }
        try {
            double parsed=Double.parseDouble(value);
            if (!Double.isFinite(parsed)) {
                throw new IllegalArgumentException("stage parameter must be finite: "+qualifiedKey(stageName, parameterName));
            }
            return parsed;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("stage parameter must be numeric: "+qualifiedKey(stageName, parameterName), exception);
        }
    }
    public String getString(String stageName, String parameterName, String defaultValue) {
        String value=overrides.get(qualifiedKey(stageName, parameterName));
        if (value==null) {
            return defaultValue;
        }
        return value;
    }
    public int[] getIntegerArray(String stageName, String parameterName, int[] defaultValue) {
        String value=getString(stageName, parameterName, null);
        if (value==null) {
            if (defaultValue==null) {
                throw new IllegalArgumentException("defaultValue must not be null");
            }
            return defaultValue.clone();
        }
        StringTokenizer tokenizer=new StringTokenizer(value, ",", false);
        int count=tokenizer.countTokens();
        if (count==0) {
            throw new IllegalArgumentException("stage parameter must contain integers: "+qualifiedKey(stageName, parameterName));
        }
        int[] values=new int[count];
        for (int index=0;index<count;index++) {
            try {
                values[index]=Integer.parseInt(tokenizer.nextToken().trim());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("stage parameter must contain integers: "+qualifiedKey(stageName, parameterName), exception);
            }
        }
        return values;
    }
    public long getConfigHash() {
        long result=clean?1L:0L;
        result=31L*result+presetName.hashCode();
        result=31L*result+rasterMode.hashCode();
        result=31L*result+overrides.hashCode();
        return result;
    }
    @Override
    public boolean equals(Object other) {
        if (this==other) {
            return true;
        }
        if (other==null||getClass()!=other.getClass()) {
            return false;
        }
        PipelineConfig config=(PipelineConfig)other;
        return clean==config.clean&&presetName.equals(config.presetName)&&rasterMode==config.rasterMode&&overrides.equals(config.overrides);
    }
    @Override
    public int hashCode() {
        int result=17;
        result=31*result+(clean?1:0);
        result=31*result+presetName.hashCode();
        result=31*result+rasterMode.hashCode();
        result=31*result+overrides.hashCode();
        return result;
    }
    @Override
    public String toString() {
        return "PipelineConfig{clean="+clean+", presetName="+presetName+", rasterMode="+rasterMode+", overrides="+overrides+"}";
    }
    private static String qualifiedKey(String stageName, String parameterName) {
        validateName(stageName, "stageName");
        validateName(parameterName, "parameterName");
        return stageName+"."+parameterName;
    }
    private static void validateKey(String key) {
        if (key==null||key.trim().isEmpty()) {
            throw new IllegalArgumentException("override keys must not be blank");
        }
        int separator=key.indexOf('.');
        if (separator<=0||separator==key.length()-1||key.indexOf('.', separator+1)>=0) {
            throw new IllegalArgumentException("override keys must use stage.parameter format");
        }
    }
    private static void validateName(String value, String name) {
        if (value==null||value.trim().isEmpty()||value.indexOf('.')>=0) {
            throw new IllegalArgumentException(name+" must be nonblank and contain no dot");
        }
    }
}
