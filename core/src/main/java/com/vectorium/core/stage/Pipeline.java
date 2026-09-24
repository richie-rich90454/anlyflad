package com.vectorium.core.stage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.vectorium.core.model.VectorDocument;
public final class Pipeline {
    private final List<Stage> stages;
    private final PipelineLogger logger;
    private final PipelineMemoizer memoizer;
    public Pipeline(List<Stage> stages, PipelineLogger logger, PipelineMemoizer memoizer) {
        if (stages==null) {
            throw new IllegalArgumentException("stages must not be null");
        }
        if (logger==null) {
            throw new IllegalArgumentException("logger must not be null");
        }
        if (memoizer==null) {
            throw new IllegalArgumentException("memoizer must not be null");
        }
        List<Stage> copiedStages=new ArrayList<Stage>(stages.size());
        for (int index=0;index<stages.size();index++) {
            Stage stage=stages.get(index);
            if (stage==null) {
                throw new IllegalArgumentException("stages must not contain null");
            }
            copiedStages.add(stage);
        }
        this.stages=Collections.unmodifiableList(copiedStages);
        this.logger=logger;
        this.memoizer=memoizer;
    }
    public VectorDocument run(VectorDocument input, PipelineConfig config) throws StageException {
        if (input==null) {
            throw new IllegalArgumentException("input must not be null");
        }
        if (config==null) {
            throw new IllegalArgumentException("config must not be null");
        }
        long configHash=config.getConfigHash();
        VectorDocument cached=memoizer.get(input, configHash);
        if (cached!=null) {
            return cached;
        }
        VectorDocument current=input;
        for (int index=0;index<stages.size();index++) {
            Stage configured=stages.get(index);
            if (configured.getTag()==StageTag.CLEANER&&!config.isClean()) {
                logger.onStage(configured.getName(), StageResult.SKIPPED, 0L);
                continue;
            }
            if (!config.isStageEnabled(configured.getName())) {
                logger.onStage(configured.getName(), StageResult.SKIPPED, 0L);
                continue;
            }
            long started=System.nanoTime();
            try {
                if (configured instanceof ConfigurableStage) {
                    configured=((ConfigurableStage)configured).withConfig(config);
                    if (configured==null) {
                        throw new StageException("configurable stage returned null");
                    }
                }
                if (!configured.appliesTo(current)) {
                    logger.onStage(configured.getName(), StageResult.SKIPPED, System.nanoTime()-started);
                    continue;
                }
                VectorDocument output=configured.apply(current);
                if (output==null) {
                    throw new StageException("stage returned null");
                }
                current=output;
                logger.onStage(configured.getName(), StageResult.APPLIED, System.nanoTime()-started);
            } catch (StageException exception) {
                logger.onStage(configured.getName(), StageResult.FAILED, System.nanoTime()-started);
                throw exception;
            } catch (RuntimeException exception) {
                logger.onStage(configured.getName(), StageResult.FAILED, System.nanoTime()-started);
                throw exception;
            }
        }
        memoizer.put(input, configHash, current);
        return current;
    }
    public List<Stage> getStages() {
        return stages;
    }
}
