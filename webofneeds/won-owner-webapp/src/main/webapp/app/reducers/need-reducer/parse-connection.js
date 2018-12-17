import Immutable from "immutable";
import won from "../../won-es6.js";

import { isUriRead } from "../../won-localstorage.js";

export function parseConnection(jsonldConnection) {
  const jsonldConnectionImm = Immutable.fromJS(jsonldConnection);

  let parsedConnection = {
    belongsToUri: undefined,
    facetUri: jsonldConnectionImm.get("hasFacet"),
    data: {
      uri: undefined,
      state: undefined,
      messages: Immutable.Map(),
      facet: undefined, //will be determined by the reduce-connections
      agreementData: {
        agreementUris: Immutable.Set(),
        pendingProposalUris: Immutable.Set(),
        pendingCancellationProposalUris: Immutable.Set(),
        cancellationPendingAgreementUris: Immutable.Set(),
        acceptedCancellationProposalUris: Immutable.Set(),
        cancelledAgreementUris: Immutable.Set(),
        rejectedMessageUris: Immutable.Set(),
        retractedMessageUris: Immutable.Set(),
        proposedMessageUris: Immutable.Set(),
        claimedMessageUris: Immutable.Set(),
        isLoaded: false,
      },
      petriNetData: Immutable.Map(),
      remoteNeedUri: undefined,
      remoteConnectionUri: undefined,
      creationDate: undefined,
      lastUpdateDate: undefined,
      unread: undefined,
      isRated: false,
      isLoadingMessages: false,
      isLoadingAgreementData: false,
      isLoadingPetriNetData: false,
      isLoading: false,
      failedToLoad: false,
      showAgreementData: false,
      showPetriNetData: false,
      multiSelectType: undefined,
    },
  };

  const belongsToUri = jsonldConnectionImm.get("belongsToNeed");
  const remoteNeedUri = jsonldConnectionImm.get("hasRemoteNeed");
  const remoteConnectionUri = jsonldConnectionImm.get("hasRemoteConnection");
  const uri = jsonldConnectionImm.get("uri");

  if (!!uri && !!belongsToUri && !!remoteNeedUri) {
    parsedConnection.belongsToUri = belongsToUri;
    parsedConnection.data.uri = uri;
    parsedConnection.data.unread = !isUriRead(uri);
    parsedConnection.data.remoteNeedUri = remoteNeedUri;
    parsedConnection.data.remoteConnectionUri = remoteConnectionUri;

    const creationDate = jsonldConnectionImm.get("modified");
    if (creationDate) {
      parsedConnection.data.creationDate = new Date(creationDate);
      parsedConnection.data.lastUpdateDate = parsedConnection.data.creationDate;
    }

    const state = jsonldConnectionImm.get("hasConnectionState");
    if (
      state === won.WON.RequestReceived ||
      state === won.WON.RequestSent ||
      state === won.WON.Suggested ||
      state === won.WON.Connected ||
      state === won.WON.Closed
    ) {
      parsedConnection.data.state = state;
    } else {
      console.error(
        "Cant parse connection, data is an invalid connection-object (faulty state): ",
        jsonldConnectionImm.toJS()
      );
      return undefined; // FOR UNKNOWN STATES
    }

    return Immutable.fromJS(parsedConnection);
  } else {
    console.error(
      "Cant parse connection, data is an invalid connection-object (mandatory uris could not be retrieved): ",
      jsonldConnectionImm.toJS()
    );
    return undefined;
  }
}
