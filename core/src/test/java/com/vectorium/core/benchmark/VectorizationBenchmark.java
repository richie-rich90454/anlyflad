package com.vectorium.core.benchmark;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import com.vectorium.core.geometry.DouglasPeucker;
import com.vectorium.core.model.RasterOrigin;
import com.vectorium.core.model.VectorDocument;
import com.vectorium.core.model.VectorPath;
import com.vectorium.core.perf.ColorLut;
import com.vectorium.core.perf.MortonCodes;
import com.vectorium.core.raster.RasterFrame;
import com.vectorium.core.raster.RasterVectorizer;
import com.vectorium.core.svg.SvgParser;
import com.vectorium.core.svg.SvgWriter;
import com.vectorium.core.stage.BoundedPipelineMemoizer;
import com.vectorium.core.stage.Pipeline;
import com.vectorium.core.stage.PipelineConfig;
import com.vectorium.core.stage.StageRegistry;
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations=1, time=100, timeUnit=TimeUnit.MILLISECONDS)
@Measurement(iterations=2, time=100, timeUnit=TimeUnit.MILLISECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class VectorizationBenchmark {
    private int width;
    private int height;
    private int[] pixels;
    private RasterFrame frame;
    private ColorLut colorLut;
    private double[] points;
    private boolean[] keep;
    private int[] stack;
    private int[] outputIndices;
    private VectorDocument document;
    private Pipeline pipeline;
    private PipelineConfig config;
    @Setup
    public void setUp() {
        width=64;
        height=64;
        pixels=new int[width*height];
        for (int y=0;y<height;y++) {
            for (int x=0;x<width;x++) {
                boolean foreground=((x/4+y/4)%2)==0;
                pixels[y*width+x]=foreground?0xFF000000:0xFFFFFFFF;
            }
        }
        frame=RasterFrame.wrap(width, height, pixels);
        colorLut=new ColorLut(new int[]{0x000000,0xFFFFFF,0x00FF00,0xFF0000});
        points=new double[2048];
        for (int index=0;index<points.length;index+=2) {
            points[index]=index/2;
            points[index+1]=Math.sin(index/2*0.1)*4.0;
        }
        keep=new boolean[points.length/2];
        stack=new int[points.length];
        outputIndices=new int[points.length/2];
        document=new VectorDocument("benchmark", new RasterOrigin("benchmark"), java.util.Collections.<VectorPath>emptyList(), width, height, pixels);
        config=PipelineConfig.defaults();
        pipeline=new StageRegistry().buildPipeline(config);
    }
    @Benchmark
    public void colorLookup(Blackhole blackhole) {
        for (int index=0;index<pixels.length;index+=16) {
            blackhole.consume(colorLut.lookupArgb(pixels[index]));
        }
    }
    @Benchmark
    public int mortonSort() {
        long[] values=new long[1024];
        long[] scratch=new long[values.length];
        for (int index=0;index<values.length;index++) {
            values[index]=MortonCodes.pack(index&255,(index>>>8)&255);
        }
        return MortonCodes.sort(values, values.length, scratch);
    }
    @Benchmark
    public int simplify(Blackhole blackhole) {
        blackhole.consume(DouglasPeucker.simplify(points, false, 0.5, keep, stack, outputIndices));
        return outputIndices[0];
    }
    @Benchmark
    public int rasterVectorize(Blackhole blackhole) {
        blackhole.consume(RasterVectorizer.vectorize(frame));
        return frame.getWidth();
    }
    @Benchmark
    public VectorDocument pipelineRun() throws Exception {
        VectorDocument input=new VectorDocument("benchmark-run", new RasterOrigin("benchmark-run"), java.util.Collections.<VectorPath>emptyList(), width, height, pixels);
        return pipeline.run(input, config);
    }
    @Benchmark
    public String svgWrite() {
        return SvgWriter.write(document);
    }
    @Benchmark
    public VectorDocument svgParse() throws Exception {
        return new SvgParser().parse("benchmark.svg", "<svg width='64' height='64'><path d='M0 0 L32 0 L32 32 Z'/></svg>");
    }
}
