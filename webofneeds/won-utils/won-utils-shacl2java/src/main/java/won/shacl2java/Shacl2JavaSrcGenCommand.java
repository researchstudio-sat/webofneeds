package won.shacl2java;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.github.rvesse.airline.SingleCommand;
import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required;
import com.github.rvesse.airline.help.cli.CliCommandUsageGenerator;
import com.github.rvesse.airline.parser.errors.ParseArgumentsMissingException;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.LoggerFactory;
import won.shacl2java.sourcegen.SourceGenerator;
import won.shacl2java.sourcegen.SourceGeneratorStats;

@Command(name = "sourcegen", description = "Generate Java classes from SHACL shapes")
public class Shacl2JavaSrcGenCommand {
    private static final String DEFAULT_OUTPUT_DIR = "generated-sources";
    private static final String DEFAULT_PACKAGE_NAME = "shacl2java";
    private static final String DEFAULT_REGEX = "(Property)?Shape$";
    private static final String DEFAULT_REPLACEMENT = "";
    @Arguments(title = "SHACL shapes files", description = "Any number of SHACL files to consider individually for generating the classes")
    @Required
    List<String> shapesFiles;
    @Option(name = { "-o",
                    "--output-dir" }, description = "The directory, relative or absolute, that the sources are generated in. "
                                    +
                                    "The directories corresponding to the packageName are created in the output directory. "
                                    +
                                    "Will be created if it does not exist. Defaults to '" + DEFAULT_OUTPUT_DIR
                                    + "'.", title = "OutputDirectory")
    private String outputDir = DEFAULT_OUTPUT_DIR;
    @Option(name = { "-p",
                    "--package-name" }, description = "The java package for all generated classes, such as 'com.example.myproject'. The appropriate "
                                    +
                                    "directory tree is generated in the output directory. Defaults to '"
                                    + DEFAULT_PACKAGE_NAME + "'.", title = "JavaPackage")
    private String packageName = DEFAULT_PACKAGE_NAME;
    @Option(name = { "-cx", "--classname-regex-replace" }, title = { "Regex",
                    "Replacement" }, arity = 2, description = "Java regular expression and replacement strings. Java class names "
                                    +
                                    "are derived from SHACL shape names after performing this optional replacement. " +
                                    "Defaults to '" + DEFAULT_REGEX + "' and '" + DEFAULT_REPLACEMENT
                                    + "', respectively.")
    private List<String> classnameRegexReplace = Stream.of(DEFAULT_REGEX, DEFAULT_REPLACEMENT)
                    .collect(Collectors.toList());
    @Option(name = { "-fi", "--always-add-field-type-indicator" }, title = {
                    "AlwaysAddFieldTypeIndicator" }, description = "Set to true always to include a type indicator in class member names. For example, "
                                    +
                                    "generate 'addressString' instead of 'address'. Does not always look great. Set this to "
                                    +
                                    "false to let the generator decide when to do it and when not. Defaults to 'false'")
    private boolean alwaysAddFieldTypeIndicator = false;
    @Option(name = { "-fa", "--abbreviate-field-type-indicators" }, title = {
                    "AbbreviateFieldTypeIndicators" }, description = "Set to true to abbreviate type indicators in class member names. For example, "
                                    +
                                    "generate 'addressS' instead of 'addressString'. Abbreviation removes the lower-case letters of "
                                    +
                                    "the type. Defaults to 'false'")
    private boolean abbreviateFieldTypeIndicators = false;
    @Option(name = { "q", "--quiet" }, title = { "Quiet" }, description = "Be quite on stdout. Default 'false'.")
    private boolean quiet = false;
    @Option(name = { "-vc", "--visitor-classes" }, title = {
                    "VisitorClasses" }, description = "Generate Code for the Visitor Pattern for shapes that have one "
                                    +
                                    "of the specified URIs as their rdf:type")
    private Set<String> visitorClasses = new HashSet<>();
    @Option(name = { "-i",
                    "--interfaces-for-rdf-types" }, title = "InterfacesForRdfTypes", description = "Generate tagging interfaces (ie, no methods) for each rdf:type of a shape that is not in the SHACL namespace.")
    private boolean interfacesForRdfTypes = true;

    public String getOutputDir() {
        return outputDir;
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public Set<String> getVisitorClasses() {
        return visitorClasses;
    }

    public boolean isInterfacesForRdfTypes() {
        return interfacesForRdfTypes;
    }

    public List<String> getClassnameRegexReplace() {
        return classnameRegexReplace;
    }

    public boolean isAlwaysAddFieldTypeIndicator() {
        return alwaysAddFieldTypeIndicator;
    }

    public boolean isAbbreviateFieldTypeIndicators() {
        return abbreviateFieldTypeIndicators;
    }

    public static void main(String[] args) {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        root.setLevel(Level.WARN);
        System.out.println(String.format("running %s %s",
                        MethodHandles.lookup().lookupClass().getName(),
                        Stream.of(args).collect(Collectors.joining(" "))));
        SingleCommand<Shacl2JavaSrcGenCommand> parser;
        Shacl2JavaSrcGenCommand cmd = null;
        parser = SingleCommand.singleCommand(Shacl2JavaSrcGenCommand.class);
        try {
            cmd = parser.parse(args);
        } catch (ParseArgumentsMissingException e) {
            System.err.println(e.getMessage());
            CliCommandUsageGenerator usageMessageGenerator = new CliCommandUsageGenerator();
            printUsage(parser, usageMessageGenerator);
            System.exit(3);
        }
        SourceGenerator generator = new SourceGenerator();
        Shacl2JavaConfig config = Shacl2JavaConfig.builder()
                        .abbreviateTypeIndicators(cmd.abbreviateFieldTypeIndicators)
                        .alwaysAddTypeIndicator(cmd.alwaysAddFieldTypeIndicator)
                        .outputDir(cmd.outputDir)
                        .classNameRegexReplace(cmd.classnameRegexReplace.get(0), cmd.classnameRegexReplace.get(1))
                        .packageName(cmd.packageName)
                        .addVisitorClassesString(cmd.visitorClasses)
                        .interfacesForRdfTypes(cmd.interfacesForRdfTypes)
                        .build();
        String currentFile = null;
        try {
            for (String shapesFile : cmd.shapesFiles) {
                currentFile = shapesFile;
                SourceGeneratorStats stats = generator.generate(new File(shapesFile), config);
                if (!cmd.quiet) {
                    System.out.println(String.format("Shacl2Java source generation stats for %s", shapesFile));
                    System.out.println(stats.formatted());
                    System.out.println(String.format("(run with option '-q' to suppress this output)", shapesFile));
                }
            }
        } catch (IOException e) {
            System.err.println(String.format(
                            "Cannot generate classes into %s from shapes file %s - %s: %s",
                            config.getOutputDir(),
                            currentFile,
                            e.getClass().getSimpleName(),
                            e.getMessage()));
            if (!cmd.quiet) {
                e.printStackTrace(System.err);
            }
            System.exit(1);
        }
    }

    public static void printUsage(SingleCommand<Shacl2JavaSrcGenCommand> parser, CliCommandUsageGenerator usage) {
        try {
            usage.usage(parser.getCommandMetadata(), parser.getParserConfiguration(), System.out);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            System.exit(2);
        }
    }
}
