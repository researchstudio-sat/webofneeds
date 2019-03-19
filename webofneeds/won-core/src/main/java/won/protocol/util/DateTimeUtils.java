package won.protocol.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: atus Date: 23.04.13
 */
public class DateTimeUtils {
    private static final Logger logger = LoggerFactory.getLogger(DateTimeUtils.class);
    private static SimpleDateFormat sdf;
    private static final String DATE_FORMAT_XSD_DATE_TIME_STAMP = "yyyy-MM-DD'T'hh:mm:ss.sssZ";

    /**
     * Formats the date as xsd:dateTimeStamp (time stamp with timezone info).
     * 
     * @param date
     * @return
     */
    // TODO: here, we're using Calendar's default time zone!! Should be the timezone used by the creator of the Date
    // object
    public static Literal toLiteral(Date date, Model model) {
        if (date == null)
            return null;
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        XSDDateTime dateTime = new XSDDateTime(cal);
        return model.createTypedLiteral(dateTime, XSDDatatype.XSDdateTime);
    }

    /**
     * Returns the current date as xsd:dateTimeStamp (time stamp with timezone info).
     * 
     * @return
     */
    public static Literal getCurrentDateTimeStamp(Model model) {
        return toLiteral(new Date(), model);
    }

    public static Date toDate(Literal literal, Model model) {
        if (XSDDatatype.XSDdateTime.equals(literal.getDatatype())) {
            XSDDateTime dateTime = (XSDDateTime) XSDDatatype.XSDdateTime.parse(literal.getLexicalForm());
            return dateTime.asCalendar().getTime();
        } else if (literal.getDatatype() == null) {
            // if the literal is not typed, try to interpret it as an xsd:dateTime
            Literal asXsdDateTime = model.createTypedLiteral(literal.getLexicalForm(), XSDDatatype.XSDdateTime);
            return toDate(asXsdDateTime, model);
        }
        return null;
    }

    /**
     * Parses the specified date, which is expected to be an xsd:dateTimeStamp (time stamp with timezone info).
     * 
     * @param date
     * @return the date or null if the format is not recognized
     */
    public static Date parse(String date, Model model) {
        return toDate(model.createTypedLiteral(date, XSDDatatype.XSDdateTime), model);
    }

    /**
     * Converts node to Date if it is a literal, returns null otherwise.
     * 
     * @param node
     * @param model
     * @return
     */
    public static Date toDate(RDFNode node, Model model) {
        if (!node.isLiteral())
            return null;
        Literal nodeAsLiteral = node.asLiteral();
        return toDate(nodeAsLiteral, model);
    }
}
