package won.shacl2java;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Shacl2JavaConfig {
    private String packageName;
    private String outputDir;
    private Pattern classNameRegex;
    private String classNameReplacement;
    /**
     * <p>
     * Set to true to include a type indicator always.
     * </p>
     * Consider
     * <p>
     * <code>private Set&lt;Address&gt; addressAs = ...</code>
     * </p>
     * The 'A' in <code>addressAs</code> is the abbreviated type indicator, without
     * it, the name would be <code>addresses</code>. Doesn't always look great.
     * Isn't necessary when there is only one field for the given property, e.g.
     * <code>ex:address</code> in the class. Set this to false to let the converter
     * decide.
     */
    private boolean alwaysAddTypeIndicator = false;
    /**
     * <p>
     * Set to true to abbreviate the type indicator.
     * </p>
     * Consider
     * <p>
     * <code>private Set&lt;Address&gt; addressAs = ...</code>
     * </p>
     * The 'A' in <code>addressAs</code> is the abbreviated type indicator. Without
     * abbreviation, it would read <code>addressAddresses</code>.
     */
    private boolean abbreviateTypeIndicators = false;
    /**
     * Defines a suffix for all 'union getters', defaults to 'Union'. The union
     * getter is a way to access a collection containing all the fields' values that
     * were generated for the same property path.
     */
    private String unionGetterSuffix = "Union";
    private String visitorSuffix = "Visitor";
    /**
     * Set of RDFS or OWL classes to generate visitors for (and corresponding
     * <code>accept(Visitor)</code> methods for)
     */
    private Set<URI> visitorClasses = new HashSet<>();
    /**
     * If true, tagging interfaces (ie, empty ones) are generated for each rdf:type
     * that a shape is given that is not in the SHACL namespace. (see also
     * <code>visitorClasses</code>).
     */
    private boolean interfacesForRdfTypes = true;

    public Shacl2JavaConfig() {
    }

    public static ConfigBuilder builder() {
        return new ConfigBuilder();
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public Pattern getClassNameRegex() {
        return classNameRegex;
    }

    public void setClassNameRegex(Pattern classNameRegex) {
        this.classNameRegex = classNameRegex;
    }

    public String getClassNameReplacement() {
        return classNameReplacement;
    }

    public void setClassNameReplacement(String classNameReplacement) {
        this.classNameReplacement = classNameReplacement;
    }

    public Set<URI> getVisitorClasses() {
        return visitorClasses;
    }

    public void setVisitorClasses(Set<URI> visitorClasses) {
        this.visitorClasses = visitorClasses;
    }

    public boolean isInterfacesForRdfTypes() {
        return interfacesForRdfTypes;
    }

    public void setInterfacesForRdfTypes(boolean interfacesForRdfTypes) {
        this.interfacesForRdfTypes = interfacesForRdfTypes;
    }

    public String getVisitorSuffix() {
        return visitorSuffix;
    }

    public void setVisitorSuffix(String visitorSuffix) {
        this.visitorSuffix = visitorSuffix;
    }

    public boolean isAlwaysAddTypeIndicator() {
        return alwaysAddTypeIndicator;
    }

    public void setAlwaysAddTypeIndicator(boolean alwaysAddTypeIndicator) {
        this.alwaysAddTypeIndicator = alwaysAddTypeIndicator;
    }

    public boolean isAbbreviateTypeIndicators() {
        return abbreviateTypeIndicators;
    }

    public void setAbbreviateTypeIndicators(boolean abbreviateTypeIndicators) {
        this.abbreviateTypeIndicators = abbreviateTypeIndicators;
    }

    public String getUnionGetterSuffix() {
        return unionGetterSuffix;
    }

    public void setUnionGetterSuffix(String unionGetterIndicator) {
        this.unionGetterSuffix = unionGetterIndicator;
    }

    public static class ConfigBuilder {
        private Shacl2JavaConfig config = new Shacl2JavaConfig();

        public ConfigBuilder packageName(String packageName) {
            config.setPackageName(packageName);
            return this;
        }

        public ConfigBuilder outputDir(String outputDir) {
            config.setOutputDir(outputDir);
            return this;
        }

        public ConfigBuilder classNameRegexReplace(String regex, String replacement) {
            config.setClassNameRegex(Pattern.compile(regex));
            config.setClassNameReplacement(replacement);
            return this;
        }

        public ConfigBuilder abbreviateTypeIndicators(boolean abbreviate) {
            config.setAbbreviateTypeIndicators(abbreviate);
            return this;
        }

        public ConfigBuilder alwaysAddTypeIndicator(boolean alwaysAdd) {
            config.setAlwaysAddTypeIndicator(alwaysAdd);
            return this;
        }

        public ConfigBuilder unionGetterSuffix(String indicator) {
            config.setUnionGetterSuffix(indicator);
            return this;
        }

        public ConfigBuilder addVisitorClass(URI visitorClass) {
            config.visitorClasses.add(visitorClass);
            return this;
        }

        public ConfigBuilder addVisitorClasses(Collection<URI> visitorClasses) {
            config.visitorClasses.addAll(visitorClasses);
            return this;
        }

        public ConfigBuilder addVisitorClassesString(Collection<String> visitorClasses) {
            config.visitorClasses.addAll(visitorClasses.stream().map(s -> URI.create(s)).collect(Collectors.toSet()));
            return this;
        }

        public ConfigBuilder visitorSuffix(String visitorSuffix) {
            config.setVisitorSuffix(visitorSuffix);
            return this;
        }

        public ConfigBuilder interfacesForRdfTypes(boolean doGenerate) {
            config.setInterfacesForRdfTypes(doGenerate);
            return this;
        }

        public Shacl2JavaConfig build() {
            return config;
        }
    }
}
