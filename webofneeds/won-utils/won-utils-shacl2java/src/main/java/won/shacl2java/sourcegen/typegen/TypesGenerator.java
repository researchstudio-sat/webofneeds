package won.shacl2java.sourcegen.typegen;

import com.squareup.javapoet.TypeSpec;
import java.util.Set;

public interface TypesGenerator {
    public Set<TypeSpec> generate();
}
