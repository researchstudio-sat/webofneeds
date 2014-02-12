package won.protocol.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * Created with IntelliJ IDEA.
 * User: gabriel
 * Date: 12.09.13
 * Time: 18:11
 * To change this template use File | Settings | File Templates.
 */
public enum FacetType {
    ControlFacet("ControlFacet"),
    OwnerFacet("OwnerFacet"),
    GroupFacet("GroupFacet"),
    CoordinatorFacet("CoordinatorFacet"),
    ParticipantFacet("ParticipantFacet"),
    BAPCCoordinatorFacet("BAPCCoordinatorFacet"),
    BAPCParticipantFacet("BAPCParticipantFacet"),
    CommentFacet("CommentFacet"),
    CommentModeratedFacet("CommentModeratedFacet"),
    CommentUnrestrictedFacet("CommentUnrestrictedFacet");

    private static final Logger logger = LoggerFactory.getLogger(BasicNeedType.class);

    public static String[] getNames() {
        String[] ret = new String[FacetType.values().length];
        int i = 0;

        for(FacetType ft : FacetType.values())
            ret[i++] = ft.getURI().toString();

        return ret;
    }

    public static FacetType getFacetType(URI uri) {
       if(uri.equals(FacetType.ControlFacet.getURI()))
        return FacetType.ControlFacet;
       else if(uri.equals(FacetType.GroupFacet.getURI()))
           return FacetType.GroupFacet;
       else if(uri.equals(FacetType.OwnerFacet.getURI()))
           return FacetType.OwnerFacet;
       else if(uri.equals(FacetType.CoordinatorFacet.getURI()))
           return FacetType.CoordinatorFacet;
       else if(uri.equals(FacetType.ParticipantFacet.getURI()))
           return FacetType.ParticipantFacet;
       else if(uri.equals(FacetType.BAPCCoordinatorFacet.getURI()))
           return FacetType.BAPCCoordinatorFacet;
       else if(uri.equals(FacetType.BAPCParticipantFacet.getURI()))
            return FacetType.BAPCParticipantFacet;
       else if(uri.equals(FacetType.CommentFacet.getURI()))
         return FacetType.CommentFacet;
       else if(uri.equals(FacetType.CommentModeratedFacet.getURI()))
         return FacetType.CommentModeratedFacet;
       else if(uri.equals(FacetType.CommentUnrestrictedFacet.getURI()))
         return FacetType.CommentUnrestrictedFacet;
       else
           return null;
    }

    private String name;

    private FacetType(String name)
    {
        this.name = name;
    }

    public URI getURI()
    {
        return URI.create(WON.BASE_URI + name);
    }
}
