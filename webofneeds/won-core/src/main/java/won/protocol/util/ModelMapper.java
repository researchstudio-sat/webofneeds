package won.protocol.util;

import org.apache.jena.rdf.model.Model;

/**
 * Created with IntelliJ IDEA. User: gabriel Date: 09.04.13 Time: 15:35 To
 * change this template use File | Settings | File Templates.
 */
public interface ModelMapper<T> {
    Model toModel(T tobject);

    T fromModel(Model model);
}