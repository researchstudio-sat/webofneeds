package won.protocol.util;

import com.hp.hpl.jena.rdf.model.Model;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 09.04.13
 * Time: 15:35
 * To change this template use File | Settings | File Templates.
 */
interface ModelMapper<T> {
    public Model toModel(T tobject);
    public T fromModel(Model model);
}