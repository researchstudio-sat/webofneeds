package won.shacl2java;

import org.junit.Test;
import won.shacl2java.sourcegen.SourceGeneratorStats;

import java.time.Duration;

public class SourceGeneratorStatsTest {
    @Test
    public void testSourceGeneratorStatsFormatter() {
        SourceGeneratorStats stats = new SourceGeneratorStats();
        stats.setGenerationDuration(Duration.ofMillis(250));
        stats.setReadShapesDuration(Duration.ofMillis(1002));
        stats.setWriteDuration(Duration.ofMillis(550));
        stats.setNumClasses(40);
        stats.setOutputDir("/tmp/shacl2java");
        stats.setShapesFile("./shapes.ttl");
        String formatted = stats.formatted();
        System.out.println(formatted);
    }
}
