package com.anlyflad.core.stage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.anlyflad.core.model.Color;
import com.anlyflad.core.model.PathId;
import com.anlyflad.core.model.StageDescriptor;
import com.anlyflad.core.model.SvgOrigin;
import com.anlyflad.core.model.VectorDocument;
import com.anlyflad.core.model.VectorPath;
public final class CleanerStagesTest {
    @Test
    public void shouldRemoveSpecksAndApplyAreaOverrides() {
        StageDescriptor descriptor=new StageDescriptor("speck-filter", "Speck Filter", "Removes specks", true);
        SpeckFilterStage stage=new SpeckFilterStage(descriptor, 2.0);
        VectorDocument document=document(rectangle(0, 0, 0, 1, 1, 1, 1, 1), rectangle(1, 2, 2, 2, 4, 2, 4, 4));
        assertEquals(1, stage.apply(document).getPaths().size());
        SpeckFilterStage configured=(SpeckFilterStage)stage.withConfig(PipelineConfig.defaults().withStageValue("speck-filter", "minArea", "0"));
        assertEquals(2, configured.apply(document).getPaths().size());
    }
    @Test
    public void shouldMergeOnlyNearbySimilarColors() {
        StageDescriptor descriptor=new StageDescriptor("color-merge", "Color Merge", "Merges colors", true);
        ColorMergeStage stage=new ColorMergeStage(descriptor, 3.0);
        VectorPath first=rectangle(0, 0, 0, 1, 1, 1, 1, 1);
        first=first.withStyle(new Color(100, 100, 100, 255), 1.0);
        VectorPath second=rectangle(1, 0, 0, 1, 1, 1, 1, 1);
        second=second.withStyle(new Color(102, 100, 100, 255), 1.0);
        VectorPath far=rectangle(2, 10, 10, 11, 11, 11, 11, 10);
        far=far.withStyle(new Color(200, 0, 0, 255), 1.0);
        VectorDocument document=document(first, second, far);
        VectorDocument output=stage.apply(document);
        assertEquals(new Color(100, 100, 100, 255), output.getPaths().get(1).getFill());
        assertEquals(new Color(200, 0, 0, 255), output.getPaths().get(2).getFill());
    }
    @Test
    public void shouldNotMergeDifferentAlphaColors() {
        StageDescriptor descriptor=new StageDescriptor("color-merge", "Color Merge", "Merges colors", true);
        ColorMergeStage stage=new ColorMergeStage(descriptor, 3.0);
        VectorPath first=rectangle(0, 0, 0, 1, 1, 1, 1, 1).withStyle(new Color(100, 100, 100, 128), 1.0);
        VectorPath second=rectangle(1, 0, 0, 1, 1, 1, 1, 1).withStyle(new Color(100, 100, 100, 255), 1.0);
        VectorDocument output=stage.apply(document(first, second));
        assertEquals(128, output.getPaths().get(0).getFill().getAlpha());
        assertEquals(255, output.getPaths().get(1).getFill().getAlpha());
    }
    @Test
    public void shouldUnionOneHundredRectanglesIntoAtMostTenPaths() {
        StageDescriptor descriptor=new StageDescriptor("union", "Union", "Unions rectangles", true);
        UnionStage stage=new UnionStage(descriptor, 0.01);
        ArrayList<VectorPath> paths=new ArrayList<VectorPath>();
        int id=0;
        for (int row=0;row<10;row++) {
            for (int column=0;column<10;column++) {
                paths.add(rectangle(id, column, row, column+1, row, column+1, row+1, column, row+1));
                id++;
            }
        }
        VectorDocument output=stage.apply(document(paths.toArray(new VectorPath[paths.size()])));
        assertTrue(output.getPaths().size()<=10);
    }
    @Test
    public void shouldNotFillDiagonalOrDifferentColorRectangles() {
        StageDescriptor descriptor=new StageDescriptor("union", "Union", "Unions rectangles", true);
        UnionStage stage=new UnionStage(descriptor, 0.0);
        VectorPath diagonal=rectangle(0, 0.0, 0.0, 1.0, 0.0, 1.0, 1.0, 0.0, 1.0);
        VectorPath other=rectangle(1, 1.0, 1.0, 2.0, 1.0, 2.0, 2.0, 1.0, 2.0);
        VectorPath different=rectangle(1, 3.0, 0.0, 4.0, 0.0, 4.0, 1.0, 3.0, 1.0).withStyle(new Color(255, 0, 0, 255), 1.0);
        VectorDocument document=document(diagonal, other, different);
        assertEquals(3, stage.apply(document).getPaths().size());
    }
    @Test
    public void shouldSimplifySmoothSortDeduplicateAndFixHoles() {
        StageDescriptor simplifyDescriptor=new StageDescriptor("simplify", "Simplify", "Simplifies", true);
        SimplifyStage simplify=new SimplifyStage(simplifyDescriptor, 1.0);
        VectorPath line=new VectorPath(PathId.of(0), new double[]{0.0, 0.0, 1.0, 1.0, 2.0, 0.0}, false, new Color(0, 0, 0, 255), 1.0);
        assertEquals(2, simplify.apply(document(line)).getPaths().get(0).getNodeCount());
        VectorPath thin=rectangle(1, 0.0, 0.0, 10.0, 0.0, 10.0, 1.0, 0.0, 1.0);
        VectorPath simplifiedThin=simplify.apply(document(thin)).getPaths().get(0);
        assertTrue(simplifiedThin.getNodeCount()>=3);
        assertEquals(10.0, simplifiedThin.getArea(), 0.0);
        StageDescriptor smoothDescriptor=new StageDescriptor("smooth", "Smooth", "Smooths", true);
        SmoothStage smooth=new SmoothStage(smoothDescriptor, 1);
        VectorPath open=new VectorPath(PathId.of(1), new double[]{0.0, 0.0, 1.0, 0.0, 2.0, 0.0}, false, new Color(0, 0, 0, 255), 1.0);
        VectorPath smoothed=smooth.apply(document(open)).getPaths().get(0);
        assertEquals(0.0, smoothed.getCoordinates()[0], 0.0);
        assertEquals(2.0, smoothed.getCoordinates()[smoothed.getCoordinates().length-2], 0.0);
        StageDescriptor sortDescriptor=new StageDescriptor("layer-sort", "Layer Sort", "Sorts", true);
        VectorPath small=rectangle(0, 0, 0, 1, 1, 1, 1, 1);
        VectorPath large=rectangle(1, 0, 0, 4, 0, 4, 4, 0, 4);
        VectorDocument sorted=new LayerSortStage(sortDescriptor, true).apply(document(small, large));
        assertEquals(16.0, sorted.getPaths().get(0).getArea(), 0.0);
        StageDescriptor dedupeDescriptor=new StageDescriptor("dedupe", "Deduplicate", "Deduplicates", true);
        VectorPath duplicate=rectangle(1, 0, 0, 0, 1, 1, 1, 1, 1);
        assertEquals(1, new DedupeStage(dedupeDescriptor, 0.0).apply(document(small, duplicate)).getPaths().size());
        StageDescriptor holeDescriptor=new StageDescriptor("hole-fix", "Hole Fix", "Fixes holes", true);
        VectorPath hole=rectangle(3, 1, 1, 2, 1, 2, 2, 1, 2);
        assertEquals(1, new HoleFixStage(holeDescriptor, 2.0).apply(document(large, hole)).getPaths().size());
    }
    @Test
    public void shouldExposeCleanerMetadata() {
        StageDescriptor descriptor=new StageDescriptor("simplify", "Simplify", "Simplifies", true);
        SimplifyStage stage=new SimplifyStage(descriptor, 1.0);
        assertEquals(StageTag.CLEANER, stage.getTag());
        assertTrue(stage.appliesTo(document()));
        assertFalse(stage.appliesTo(null));
        assertSame(stage, stage.withConfig(PipelineConfig.defaults()));
    }
    private VectorDocument document(VectorPath... paths) {
        List<VectorPath> list=new ArrayList<VectorPath>();
        for (int index=0;index<paths.length;index++) {
            list.add(paths[index]);
        }
        return new VectorDocument("clean", new SvgOrigin("clean"), list, 20, 20, new int[400]);
    }
    private VectorPath rectangle(double minX, double minY, double maxX, double topY, double rightX, double maxY, double leftX, double bottomY) {
        return rectangle(PathId.zero().getValue(), minX, minY, maxX, topY, rightX, maxY, leftX, bottomY);
    }
    private VectorPath rectangle(int id, double minX, double minY, double maxX, double topY, double rightX, double maxY, double leftX, double bottomY) {
        return new VectorPath(PathId.of(id), new double[]{minX, minY, maxX, topY, rightX, maxY, leftX, bottomY}, true, new Color(0, 0, 0, 255), 1.0);
    }
}
