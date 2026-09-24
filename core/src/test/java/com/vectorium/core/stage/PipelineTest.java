package com.vectorium.core.stage;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import com.vectorium.core.model.VectorDocument;
public final class PipelineTest {
    @Test
    public void shouldApplyStagesAndLogTimings() throws Exception {
        VectorDocument input=document("input", 1);
        RecordingLogger logger=new RecordingLogger();
        Pipeline pipeline=new Pipeline(Arrays.<Stage>asList(new ClearPathsStage(StageTag.UNIVERSAL), new ClearPathsStage(StageTag.CLEANER)), logger, new BoundedPipelineMemoizer());
        VectorDocument output=pipeline.run(input, PipelineConfig.defaults());
        assertEquals(0, output.getPaths().size());
        assertEquals(2, logger.size());
        assertEquals(StageResult.APPLIED, logger.getResult(0));
        assertEquals("universal", logger.getName(0));
        assertEquals("cleaner", logger.getName(1));
    }
    @Test
    public void shouldHonorCleanerAndPerStageToggles() throws Exception {
        VectorDocument input=document("input", 1);
        RecordingLogger logger=new RecordingLogger();
        Pipeline pipeline=new Pipeline(Arrays.<Stage>asList(new ClearPathsStage(StageTag.UNIVERSAL), new ClearPathsStage(StageTag.CLEANER)), logger, new BoundedPipelineMemoizer());
        VectorDocument noClean=pipeline.run(input, PipelineConfig.defaults().withClean(false));
        assertEquals(0, noClean.getPaths().size());
        assertEquals(StageResult.SKIPPED, logger.getResult(1));
        logger.clear();
        PipelineConfig disabled=PipelineConfig.defaults().withStageDisabled("universal");
        VectorDocument disabledOutput=pipeline.run(document("disabled", 1), disabled);
        assertEquals(0, disabledOutput.getPaths().size());
        assertEquals(StageResult.SKIPPED, logger.getResult(0));
    }
    @Test
    public void shouldSkipStagesThatDoNotApply() throws Exception {
        RecordingLogger logger=new RecordingLogger();
        Pipeline pipeline=new Pipeline(Arrays.<Stage>asList(new RasterOnlyStage()), logger, new BoundedPipelineMemoizer());
        VectorDocument output=pipeline.run(document("svg", 1), PipelineConfig.defaults());
        assertEquals(1, output.getPaths().size());
        assertEquals(StageResult.SKIPPED, logger.getResult(0));
    }
    @Test
    public void shouldMemoizeAndRemainReentrant() throws Exception {
        RecordingLogger logger=new RecordingLogger();
        Pipeline pipeline=new Pipeline(Arrays.<Stage>asList(new SizeStage()), logger, new BoundedPipelineMemoizer());
        VectorDocument firstInput=document("first", 1);
        VectorDocument secondInput=document("second", 1);
        PipelineConfig config=PipelineConfig.defaults().withStageValue("size", "amount", "2");
        VectorDocument first=pipeline.run(firstInput, config);
        VectorDocument firstAgain=pipeline.run(firstInput, config);
        VectorDocument second=pipeline.run(secondInput, config);
        assertSame(first, firstAgain);
        assertEquals(2, first.getWidth());
        assertEquals(2, second.getWidth());
        assertEquals(2, logger.size());
    }
    @Test
    public void shouldLogFailuresAndPropagateThem() {
        RecordingLogger logger=new RecordingLogger();
        Pipeline pipeline=new Pipeline(Arrays.<Stage>asList(new FailingStage()), logger, new BoundedPipelineMemoizer());
        try {
            pipeline.run(document("input", 1), PipelineConfig.defaults());
            fail("Expected stage failure");
        } catch (StageException exception) {
            assertEquals("failure", exception.getMessage());
        }
        assertEquals(1, logger.size());
        assertEquals(StageResult.FAILED, logger.getResult(0));
    }
    @Test
    public void shouldValidatePipelineArgumentsAndStageOutputs() {
        try {
            new Pipeline(null, new SilentPipelineLogger(), new BoundedPipelineMemoizer());
            fail("Expected null stages to be rejected");
        } catch (IllegalArgumentException exception) {
            assertTrue(exception.getMessage().length()>0);
        }
        Pipeline pipeline=new Pipeline(Arrays.<Stage>asList(new NullResultStage()), new SilentPipelineLogger(), new BoundedPipelineMemoizer());
        try {
            pipeline.run(document("input", 1), PipelineConfig.defaults());
            fail("Expected null stage output to be rejected");
        } catch (StageException exception) {
            assertEquals("stage returned null", exception.getMessage());
        }
    }
    private static VectorDocument document(String id, int pathCount) {
        java.util.List<com.vectorium.core.model.VectorPath> paths=new java.util.ArrayList<com.vectorium.core.model.VectorPath>();
        for (int index=0;index<pathCount;index++) {
            paths.add(new com.vectorium.core.model.VectorPath(com.vectorium.core.model.PathId.of(index), new double[]{0.0, 0.0, 1.0, 0.0, 1.0, 1.0}, true, new com.vectorium.core.model.Color(0, 0, 0, 255), 1.0));
        }
        return new VectorDocument(id, new com.vectorium.core.model.SvgOrigin(id), paths, 1, 1, new int[1]);
    }
    private static final class RecordingLogger implements PipelineLogger {
        private final List<String> names=new java.util.ArrayList<String>();
        private final List<StageResult> results=new java.util.ArrayList<StageResult>();
        private final List<Long> timings=new java.util.ArrayList<Long>();
        public void onStage(String stageName, StageResult result, long nanos) {
            names.add(stageName);
            results.add(result);
            timings.add(Long.valueOf(nanos));
        }
        private int size() {
            return names.size();
        }
        private String getName(int index) {
            return names.get(index);
        }
        private StageResult getResult(int index) {
            return results.get(index);
        }
        private long getTiming(int index) {
            return timings.get(index).longValue();
        }
        private void clear() {
            names.clear();
            results.clear();
            timings.clear();
        }
    }
    private static final class ClearPathsStage implements Stage {
        private final StageTag tag;
        private final String name;
        private ClearPathsStage(StageTag tag) {
            this.tag=tag;
            this.name=tag==StageTag.CLEANER?"cleaner":"universal";
        }
        public String getName() {
            return name;
        }
        public String getLabel() {
            return name;
        }
        public String getDescription() {
            return name;
        }
        public StageTag getTag() {
            return tag;
        }
        public boolean appliesTo(VectorDocument document) {
            return true;
        }
        public VectorDocument apply(VectorDocument document) {
            return document.withPaths(java.util.Collections.<com.vectorium.core.model.VectorPath>emptyList());
        }
    }
    private static final class RasterOnlyStage implements Stage {
        public String getName() {
            return "raster";
        }
        public String getLabel() {
            return "raster";
        }
        public String getDescription() {
            return "raster";
        }
        public StageTag getTag() {
            return StageTag.RASTER_ONLY;
        }
        public boolean appliesTo(VectorDocument document) {
            return document.getOrigin().isRaster();
        }
        public VectorDocument apply(VectorDocument document) {
            return document;
        }
    }
    private static final class SizeStage implements ConfigurableStage {
        private final int amount;
        private SizeStage() {
            this(1);
        }
        private SizeStage(int amount) {
            this.amount=amount;
        }
        public Stage withConfig(PipelineConfig config) {
            int configured=config.getInteger("size", "amount", amount);
            if (configured==amount) {
                return this;
            }
            return new SizeStage(configured);
        }
        public String getName() {
            return "size";
        }
        public String getLabel() {
            return "size";
        }
        public String getDescription() {
            return "size";
        }
        public StageTag getTag() {
            return StageTag.UNIVERSAL;
        }
        public boolean appliesTo(VectorDocument document) {
            return true;
        }
        public VectorDocument apply(VectorDocument document) {
            return document.withSize(amount, document.getHeight());
        }
    }
    private static final class FailingStage implements Stage {
        public String getName() {
            return "failure";
        }
        public String getLabel() {
            return "failure";
        }
        public String getDescription() {
            return "failure";
        }
        public StageTag getTag() {
            return StageTag.UNIVERSAL;
        }
        public boolean appliesTo(VectorDocument document) {
            return true;
        }
        public VectorDocument apply(VectorDocument document) throws StageException {
            throw new StageException("failure");
        }
    }
    private static final class NullResultStage implements Stage {
        public String getName() {
            return "null";
        }
        public String getLabel() {
            return "null";
        }
        public String getDescription() {
            return "null";
        }
        public StageTag getTag() {
            return StageTag.UNIVERSAL;
        }
        public boolean appliesTo(VectorDocument document) {
            return true;
        }
        public VectorDocument apply(VectorDocument document) {
            return null;
        }
    }
}
