package com.vectorium.cli;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import com.vectorium.core.model.ParamSpec;
import com.vectorium.core.model.ParamType;
import com.vectorium.core.model.StageDescriptor;
import com.vectorium.core.stage.PipelineConfig;
import com.vectorium.core.stage.RasterMode;
import com.vectorium.core.stage.StandardStageDescriptors;
public final class CliConfiguration {
    private static final List<String> PRESETS=Collections.unmodifiableList(Arrays.asList("default", "clean", "fast", "accurate"));
    public static PipelineConfig create(boolean clean, String presetName, List<String> stageValues, List<String> disabledStages) {
        return create(clean, presetName, RasterMode.COLOR, stageValues, disabledStages);
    }
    public static PipelineConfig create(boolean clean, String presetName, RasterMode rasterMode, List<String> stageValues, List<String> disabledStages) {
        if (stageValues==null||disabledStages==null) {
            throw new IllegalArgumentException("stage option lists must not be null");
        }
        String normalizedPreset=normalizePreset(presetName);
        PipelineConfig config=new PipelineConfig(clean, normalizedPreset, rasterMode, Collections.<String, String>emptyMap());
        config=applyPreset(config, normalizedPreset);
        for (int index=0;index<stageValues.size();index++) {
            config=applyStageValue(config, stageValues.get(index));
        }
        for (int index=0;index<disabledStages.size();index++) {
            String stageName=disabledStages.get(index);
            StandardStageDescriptors.get(stageName);
            if ("validate".equals(stageName)) {
                throw new IllegalArgumentException("The validate stage cannot be disabled because it is required for safe processing");
            }
            if ("serialize".equals(stageName)) {
                throw new IllegalArgumentException("The serialize stage cannot be disabled because it produces the output file");
            }
            if ("vectorize".equals(stageName)) {
                throw new IllegalArgumentException("The vectorize stage cannot be disabled because it produces the vector output");
            }
            config=config.withStageDisabled(stageName);
        }
        return config;
    }
    private static String normalizePreset(String presetName) {
        if (presetName==null||presetName.trim().isEmpty()) {
            throw new IllegalArgumentException("preset must not be blank");
        }
        String normalized=presetName.trim().toLowerCase(Locale.ROOT);
        if (!PRESETS.contains(normalized)) {
            throw new IllegalArgumentException("Unknown preset: "+presetName);
        }
        return normalized;
    }
    private static PipelineConfig applyPreset(PipelineConfig config, String presetName) {
        if (presetName.equals("fast")) {
            return config.withStageValue("simplify", "tolerance", "2.0").withStageValue("smooth", "passes", "0").withStageValue("dedupe", "tolerance", "0.5");
        }
        if (presetName.equals("accurate")) {
            return config.withStageValue("simplify", "tolerance", "0.25").withStageValue("smooth", "passes", "2");
        }
        return config;
    }
    private static PipelineConfig applyStageValue(PipelineConfig config, String assignment) {
        if (assignment==null) {
            throw new IllegalArgumentException("stage value must not be null");
        }
        int separator=assignment.indexOf('=');
        if (separator<=0||separator==assignment.length()-1) {
            throw new IllegalArgumentException("Stage values must use stage.parameter=value: "+assignment);
        }
        String key=assignment.substring(0, separator);
        int dot=key.indexOf('.');
        if (dot<=0||dot==key.length()-1||key.indexOf('.', dot+1)>=0) {
            throw new IllegalArgumentException("Stage values must use stage.parameter=value: "+assignment);
        }
        String stageName=key.substring(0, dot);
        String parameterName=key.substring(dot+1);
        String value=assignment.substring(separator+1);
        StageDescriptor descriptor=StandardStageDescriptors.get(stageName);
        ParamSpec parameter=findParameter(descriptor, parameterName);
        validateValue(parameter, value);
        return config.withStageValue(stageName, parameterName, value);
    }
    private static ParamSpec findParameter(StageDescriptor descriptor, String parameterName) {
        for (int index=0;index<descriptor.getParameters().size();index++) {
            ParamSpec parameter=descriptor.getParameters().get(index);
            if (parameter.getName().equals(parameterName)) {
                return parameter;
            }
        }
        throw new IllegalArgumentException("Unknown stage parameter: "+descriptor.getName()+"."+parameterName);
    }
    private static void validateValue(ParamSpec parameter, String value) {
        if (value.trim().isEmpty()) {
            throw new IllegalArgumentException("Stage parameter must not be blank: "+parameter.getName());
        }
        if (parameter.getType()==ParamType.BOOLEAN) {
            if (!value.equalsIgnoreCase("true")&&!value.equalsIgnoreCase("false")) {
                throw new IllegalArgumentException("Stage parameter must be true or false: "+parameter.getName());
            }
            return;
        }
        double numeric=0.0;
        if (parameter.getType()==ParamType.INTEGER) {
            try {
                numeric=Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Stage parameter must be an integer: "+parameter.getName(), exception);
            }
        } else if (parameter.getType()==ParamType.DOUBLE) {
            try {
                numeric=Double.parseDouble(value);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Stage parameter must be numeric: "+parameter.getName(), exception);
            }
            if (!Double.isFinite(numeric)) {
                throw new IllegalArgumentException("Stage parameter must be finite: "+parameter.getName());
            }
        }
        if (parameter.getMin()!=null&&numeric<parameter.getMin().doubleValue()) {
            throw new IllegalArgumentException("Stage parameter is below its minimum: "+parameter.getName());
        }
        if (parameter.getMax()!=null&&numeric>parameter.getMax().doubleValue()) {
            throw new IllegalArgumentException("Stage parameter exceeds its maximum: "+parameter.getName());
        }
    }
    private CliConfiguration() {
    }
}
