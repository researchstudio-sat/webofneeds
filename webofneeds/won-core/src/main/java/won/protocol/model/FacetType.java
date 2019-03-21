package won.protocol.model;

import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * Created with IntelliJ IDEA. User: gabriel Date: 12.09.13 Time: 18:11 To
 * change this template use File | Settings | File Templates.
 */
public enum FacetType {
  ChatFacet("ChatFacet"), HolderFacet("HolderFacet"), HoldableFacet("HoldableFacet"), GroupFacet("GroupFacet"),
  ReviewFacet("ReviewFacet"), CoordinatorFacet("CoordinatorFacet"), ParticipantFacet("ParticipantFacet"),
  CommentFacet("CommentFacet"), CommentModeratedFacet("CommentModeratedFacet"),
  CommentUnrestrictedFacet("CommentUnrestrictedFacet"), ControlFacet("ControlFacet"),
  BAPCCoordinatorFacet("BAPCCoordinatorFacet"), BAPCParticipantFacet("BAPCParticipantFacet"),
  BACCCoordinatorFacet("BACCCoordinatorFacet"), BACCParticipantFacet("BACCParticipantFacet"),
  BAAtomicPCCoordinatorFacet("BAAtomicPCCoordinatorFacet"), BAAtomicCCCoordinatorFacet("BAAtomicCCCoordinatorFacet");

  public static String[] getNames() {
    String[] ret = new String[FacetType.values().length];
    int i = 0;

    for (FacetType ft : FacetType.values())
      ret[i++] = ft.getURI().toString();

    return ret;
  }

  public static FacetType getFacetType(URI uri) {
    if (uri.equals(FacetType.ControlFacet.getURI()))
      return FacetType.ControlFacet;
    else if (uri.equals(FacetType.GroupFacet.getURI()))
      return FacetType.GroupFacet;
    else if (uri.equals(FacetType.ReviewFacet.getURI()))
      return FacetType.ReviewFacet;
    else if (uri.equals(FacetType.ChatFacet.getURI()))
      return FacetType.ChatFacet;
    else if (uri.equals(FacetType.HolderFacet.getURI()))
      return FacetType.HolderFacet;
    else if (uri.equals(FacetType.HoldableFacet.getURI()))
      return FacetType.HoldableFacet;
    else if (uri.equals(FacetType.CoordinatorFacet.getURI()))
      return FacetType.CoordinatorFacet;
    else if (uri.equals(FacetType.ParticipantFacet.getURI()))
      return FacetType.ParticipantFacet;
    else if (uri.equals(FacetType.BAPCCoordinatorFacet.getURI()))
      return FacetType.BAPCCoordinatorFacet;
    else if (uri.equals(FacetType.BAPCParticipantFacet.getURI()))
      return FacetType.BAPCParticipantFacet;
    else if (uri.equals(FacetType.BACCCoordinatorFacet.getURI()))
      return FacetType.BACCCoordinatorFacet;
    else if (uri.equals(FacetType.BACCParticipantFacet.getURI()))
      return FacetType.BACCParticipantFacet;
    else if (uri.equals(FacetType.BAAtomicPCCoordinatorFacet.getURI()))
      return FacetType.BAAtomicPCCoordinatorFacet;
    else if (uri.equals(FacetType.BAAtomicCCCoordinatorFacet.getURI()))
      return FacetType.BAAtomicCCCoordinatorFacet;
    else if (uri.equals(FacetType.CommentFacet.getURI()))
      return FacetType.CommentFacet;
    else if (uri.equals(FacetType.CommentModeratedFacet.getURI()))
      return FacetType.CommentModeratedFacet;
    else if (uri.equals(FacetType.CommentUnrestrictedFacet.getURI()))
      return FacetType.CommentUnrestrictedFacet;
    else {
      return null;
    }
  }

  private String name;

  private FacetType(String name) {
    this.name = name;
  }

  public URI getURI() {
    return URI.create(WON.BASE_URI + name);
  }
}
