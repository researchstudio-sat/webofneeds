package won.shacl2java.sourcegen.typegen;

import com.squareup.javapoet.TypeSpec;

import java.util.Map;
import java.util.Set;

public interface TypesPostprocessor {
    /**
     * For those typeSpecs that change, return an entry in the map, with the
     * original typeSpec as key, the new one as value.
     * 
     * @param typeSpecs
     * @return
     */
    public Map<TypeSpec, TypeSpec> postprocess(Set<TypeSpec> typeSpecs);
}
