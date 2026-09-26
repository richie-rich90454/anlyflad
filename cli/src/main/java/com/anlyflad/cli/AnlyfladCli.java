package com.anlyflad.cli;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import com.anlyflad.core.model.VectorDocument;
import com.anlyflad.core.stage.BoundedPipelineMemoizer;
import com.anlyflad.core.stage.Pipeline;
import com.anlyflad.core.stage.PipelineConfig;
import com.anlyflad.core.stage.RasterMode;
import com.anlyflad.core.stage.StageException;
import com.anlyflad.core.stage.StageRegistry;
import com.anlyflad.core.stage.VectorMode;
import com.anlyflad.core.svg.SvgCache;
import com.anlyflad.core.svg.SvgParseException;
@Command(name="anlyflad", mixinStandardHelpOptions=true, version="1.0.0", description="Convert PNG, JPEG, and SVG files to SVG.")
public final class AnlyfladCli {
    static {
        java.util.Locale.setDefault(java.util.Locale.Category.DISPLAY, java.util.Locale.ENGLISH);
        java.util.Locale.setDefault(java.util.Locale.Category.FORMAT, java.util.Locale.ENGLISH);
        java.util.Locale.setDefault(java.util.Locale.ENGLISH);
        com.anlyflad.core.raster.ColorCurveVectorizer.setParallelRunner(new JvmParallelRunner());
    }
    private static final int USER_ERROR=1;
    private static final int INTERNAL_ERROR=2;
    @Parameters(index="0", paramLabel="INPUT", description="Input PNG, JPEG, or SVG file.")
    private File input;
    @Option(names={"-o", "--output"}, required=true, paramLabel="FILE", description="Output SVG file.")
    private File output;
    @Option(names="--preset", paramLabel="NAME", description="Preset: default, clean, fast, or accurate.")
    private String preset="default";
    @Option(names="--mode", paramLabel="MODE", description="Raster mode: color or binary.")
    private String mode="color";
    @Option(names="--vector-mode", paramLabel="MODE", description="Vector mode: exact, contour, or curve.")
    private String vectorMode="curve";
    @Option(names="--scale", paramLabel="NAME", description="Quality scale: 0 to 100, or draft, balanced, or max.")
    private String scale;
    @Option(names="--no-clean", description="Disable cleaner stages.")
    private boolean noClean;
    @Option(names="--stage", paramLabel="STAGE.PARAM=VALUE", description="Override a stage parameter. Repeatable.")
    private List<String> stageValues=new ArrayList<String>();
    @Option(names="--no-stage", paramLabel="STAGE", description="Disable a stage. Repeatable.")
    private List<String> disabledStages=new ArrayList<String>();
    private final PrintStream stdout;
    private final PrintStream stderr;
    private final PrintWriter commandOut;
    private final PrintWriter commandErr;
    public AnlyfladCli() {
        this(System.out, System.err);
    }
    AnlyfladCli(PrintStream stdout, PrintStream stderr) {
        if (stdout==null||stderr==null) {
            throw new IllegalArgumentException("CLI streams must not be null");
        }
        this.stdout=stdout;
        this.stderr=stderr;
        this.commandOut=new PrintWriter(stdout, true);
        this.commandErr=new PrintWriter(stderr, true);
    }
    public static void main(String[] args) {
        int exitCode=new AnlyfladCli().execute(args);
        if (exitCode!=0) {
            System.exit(exitCode);
        }
    }
    public int execute(String... args) {
        resetOptions();
        CommandLine commandLine=new CommandLine(this);
        commandLine.setOut(commandOut);
        commandLine.setErr(commandErr);
        CommandLine.ParseResult parsed;
        try {
            parsed=commandLine.parseArgs(args);
        } catch (ParameterException exception) {
            try {
                commandLine.getParameterExceptionHandler().handleParseException(exception, args);
            } catch (Exception handlerException) {
                commandErr.println("Error: "+message(handlerException));
            }
            commandErr.flush();
            return USER_ERROR;
        }
        if (parsed.isUsageHelpRequested()) {
            commandLine.usage(commandOut);
            commandOut.flush();
            return 0;
        }
        if (parsed.isVersionHelpRequested()) {
            commandLine.printVersionHelp(commandOut);
            commandOut.flush();
            return 0;
        }
        try {
            convert();
            return 0;
        } catch (UserInputException exception) {
            stderr.println("Error: "+message(exception));
            return USER_ERROR;
        } catch (IOException exception) {
            stderr.println("Error: "+message(exception));
            return USER_ERROR;
        } catch (SvgParseException exception) {
            stderr.println("Error: "+message(exception));
            return USER_ERROR;
        } catch (SecurityException exception) {
            stderr.println("Error: "+message(exception));
            return USER_ERROR;
        } catch (StageException exception) {
            if (exception.isUserError()) {
                stderr.println("Error: "+message(exception));
                return USER_ERROR;
            }
            stderr.println("Internal error: "+message(exception));
            return INTERNAL_ERROR;
        } catch (RuntimeException exception) {
            stderr.println("Internal error: "+message(exception));
            return INTERNAL_ERROR;
        }
    }
    private void convert() throws IOException, SvgParseException, StageException, UserInputException {
        validateOutput();
        PipelineConfig config=createConfiguration();
        VectorDocument document=new CliDocumentLoader().load(input);
        stdout.println("Anlyflad "+config.getPresetName()+" preset, "+config.getRasterMode().getOptionName()+" raster mode, "+config.getVectorMode().getOptionName()+" vector mode");
        SvgCache cache=new SvgCache();
        CliLogger logger=new CliLogger(stdout);
        StageRegistry registry=new StageRegistry(logger, new BoundedPipelineMemoizer(), cache);
        Pipeline pipeline=registry.buildPipeline(config);
        VectorDocument result=pipeline.run(document, config);
        byte[] bytes=cache.get(result).getBytes(StandardCharsets.UTF_8);
        Path parent=output.toPath().toAbsolutePath().getParent();
        if (parent!=null) {
            Files.createDirectories(parent);
        }
        Files.write(output.toPath(), bytes);
        stdout.println("Result: paths="+result.getPaths().size()+", width="+result.getWidth()+", height="+result.getHeight()+", bytes="+bytes.length);
    }
    private PipelineConfig createConfiguration() throws UserInputException {
        try {
            List<String> values=new ArrayList<String>(stageValues);
            if (scale!=null) {
                int quality=com.anlyflad.core.stage.QualityScale.parse(scale);
                values.add("vectorize.quality="+quality);
                values.add("quantize.maxColors="+com.anlyflad.core.stage.QualityScale.maxColors(quality));
            }
            return CliConfiguration.create(!noClean, preset, RasterMode.parse(mode), VectorMode.parse(vectorMode), values, disabledStages);
        } catch (IllegalArgumentException exception) {
            throw new UserInputException(message(exception), exception);
        }
    }
    private void validateOutput() throws UserInputException {
        if (input==null||output==null) {
            throw new UserInputException("Input and output must be set");
        }
        if (!output.getName().toLowerCase(Locale.ROOT).endsWith(".svg")) {
            throw new UserInputException("Output must use the .svg extension");
        }
    }
    private void resetOptions() {
        input=null;
        output=null;
        preset="default";
        mode="color";
        vectorMode="curve";
        scale=null;
        noClean=false;
        stageValues.clear();
        disabledStages.clear();
    }
    private static String message(Throwable throwable) {
        String value=throwable.getMessage();
        if (value==null||value.trim().isEmpty()) {
            return throwable.getClass().getSimpleName();
        }
        return value;
    }
    private static final class UserInputException extends Exception {
        private static final long serialVersionUID=1L;
        private UserInputException(String message) {
            super(message);
        }
        private UserInputException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
