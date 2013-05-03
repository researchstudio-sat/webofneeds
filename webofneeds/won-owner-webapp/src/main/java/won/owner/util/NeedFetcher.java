package won.owner.util;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.RDF;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.http.client.utils.URIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.owner.pojo.NeedPojo;
import won.protocol.model.Need;
import won.protocol.model.NeedState;
import won.protocol.model.WON;
import won.protocol.rest.LinkedDataRestClient;
import won.protocol.util.LDP;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created with IntelliJ IDEA.
 * User: MrM
 * Date: 19.04.13
 * Time: 15:59
 * To change this template use File | Settings | File Templates.
 */
public class NeedFetcher {

    private final static Logger logger = LoggerFactory.getLogger(NeedFetcher.class);
    private final static LinkedDataRestClient  restClient = new LinkedDataRestClient();

    public static NeedPojo getNeedInfo(final Need slimNeed)
    {



        NeedPojo need = new NeedPojo();
        need.setNeedURI(slimNeed.getNeedURI().toString());
        need.setNeedId(slimNeed.getId());
        need.setActive(slimNeed.getState().equals(NeedState.ACTIVE));
        Model m = restClient.readResourceData(slimNeed.getNeedURI());

        String desc = "";
        String date = "";
        double lat = 0;
        double lon = 0;
        ResIterator it = m.listSubjectsWithProperty(RDF.type, WON.NEED_DESCRIPTION);
        if (it.hasNext()){
            Resource mainContentNode = it.next();
            desc = m.getProperty(mainContentNode, WON.TEXT_DESCRIPTION).getString();
        }
        need.setTextDescription(desc);




        String whereUri = "http://www.w3.org/2003/01/geo/wgs84_pos#Point";
        String latUri = "http://www.w3.org/2003/01/geo/wgs84_pos#latitude";
        String longUri = "http://www.w3.org/2003/01/geo/wgs84_pos#longitude";
        Property whereProp = m.createProperty(whereUri);
        Property latitudeProp = m.createProperty(latUri);
        Property longitudeProp = m.createProperty(longUri);



        it = m.listSubjectsWithProperty(RDF.type, whereProp);
        if (it.hasNext()) {
            Resource mainContentNode = it.next();
            lat = m.getProperty(mainContentNode,latitudeProp).getDouble();
            lon = m.getProperty(mainContentNode,longitudeProp).getDouble();
        }
        need.setLatitude(lat);
        need.setLongitude(lon);

        String timeUri = "http://www.w3.org/2006/time#";
        Property timeProp = m.createProperty(timeUri
                + "DateTimeDescription");
        Property minuteProp = m.createProperty(timeUri + "minute");
        Property hourProp = m.createProperty(timeUri + "hour");
        Property dayProp = m.createProperty(timeUri + "day");
        Property monthProp = m.createProperty(timeUri + "month");
        Property yearProp = m.createProperty(timeUri + "year");
        int minute = 0;
        int hour = 0;
        int day = 0;
        int month = 0;
        int year = 0;

        it = m.listSubjectsWithProperty(RDF.type, timeProp);
        if (it.hasNext()) {
            Resource mainContentNode = it.next();
            minute = m.getProperty(mainContentNode,minuteProp).getInt();
            hour = m.getProperty(mainContentNode,hourProp).getInt();
            day = m.getProperty(mainContentNode,dayProp).getInt();
            month = m.getProperty(mainContentNode,monthProp).getInt();
            year = m.getProperty(mainContentNode,yearProp).getInt();
        }
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day, hour, minute);


        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
        need.setDate(sdf.format(cal.getTime()));


        return need;
    }
}
