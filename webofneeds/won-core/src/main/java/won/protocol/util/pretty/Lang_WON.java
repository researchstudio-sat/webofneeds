package won.protocol.util.pretty;

import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFFormatVariant;
import org.apache.jena.riot.RDFWriterRegistry;

public class Lang_WON {
    static {
        RDFWriterRegistry.register(Lang_WON.TRIG_WON_CONVERSATION,
                        new ConversationDatasetWriterFactory());
    }
    public static final RDFFormat TRIG_WON_CONVERSATION = new RDFFormat(Lang.TRIG,
                    new RDFFormatVariant("TriG_WoN_Conversation"));

    public static void init() {
        // just give the client the chance to trigger the static initializer
    }
}
