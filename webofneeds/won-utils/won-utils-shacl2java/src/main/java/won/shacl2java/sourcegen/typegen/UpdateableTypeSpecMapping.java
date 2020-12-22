package won.shacl2java.sourcegen.typegen;

import com.squareup.javapoet.TypeSpec;
import java.util.Map;

public interface UpdateableTypeSpecMapping {
    public void applyChanges(Map<TypeSpec, TypeSpec> changedTypes);
}
