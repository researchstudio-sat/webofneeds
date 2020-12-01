package won.shacl2java.sourcegen;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SourceGeneratorStats {
    private int numClasses;
    private String outputDir;
    private Duration readShapesDuration;
    private Duration generationDuration;
    private String shapesFile;
    private Duration writeDuration;

    public void setNumClasses(int numClasses) {
        this.numClasses = numClasses;
    }

    public int getNumClasses() {
        return numClasses;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setReadShapesDuration(Duration readShapesDuration) {
        this.readShapesDuration = readShapesDuration;
    }

    public Duration getReadShapesDuration() {
        return readShapesDuration;
    }

    public void setGenerationDuration(Duration generationDuration) {
        this.generationDuration = generationDuration;
    }

    public Duration getGenerationDuration() {
        return generationDuration;
    }

    public void setShapesFile(String shapesFile) {
        this.shapesFile = shapesFile;
    }

    public String getShapesFile() {
        return shapesFile;
    }

    public String formatted() {
        return Stream.of(
                        formatStringValue("Shapes file", shapesFile),
                        formatStringValue("Output directory", outputDir),
                        formatIntValue("Classes generated", numClasses),
                        formatDurationValue("Time reading shapes", readShapesDuration),
                        formatDurationValue("Time generating classes", generationDuration),
                        formatDurationValue("Time writing classes", writeDuration))
                        .collect(Collectors.joining("\n"));
    }

    public String formatDurationValue(String label, Duration duration) {
        return String.format("%-30s %ds %dms", label + ":", duration.getSeconds(),
                        duration.toMillis() - duration.getSeconds() * 1000);
    }

    public String formatIntValue(String label, int value) {
        return String.format("%-30s %d", label + ":", value);
    }

    public String formatStringValue(String string, String shapesFile) {
        return String.format("%-30s %s", string + ":", shapesFile);
    }

    public void setWriteDuration(Duration writeDuration) {
        this.writeDuration = writeDuration;
    }

    public Duration getWriteDuration() {
        return writeDuration;
    }
}
