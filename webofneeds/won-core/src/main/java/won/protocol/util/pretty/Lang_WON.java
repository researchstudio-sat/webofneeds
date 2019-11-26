package won.protocol.util.pretty;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFFormatVariant;

public class Lang_WON {
    public static final RDFFormat TRIG_WON_CONVERSATION = new RDFFormat(Lang.TRIG,
                    new RDFFormatVariant("TriG_WoN_Conversation"));
}
