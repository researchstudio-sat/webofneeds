import Immutable from "immutable";
import won from "../../won-es6.js";

import { isUriRead } from "../../won-localstorage.js";

export function parseConnection(jsonldConnection) {
  const jsonldConnectionImm = Immutable.fromJS(jsonldConnection);

  let parsedConnection = {
    belongsToUri: jsonldConnectionImm.get("sourceAtom"),
    data: {
      uri: jsonldConnectionImm.get("uri"),
      state: jsonldConnectionImm.get("connectionState"),
      messages: Immutable.Map(),
      socketUri: jsonldConnectionImm.get("socket"),
      targetSocketUri: jsonldConnectionImm.get("targetSocket"),
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
      },
      petriNetData: Immutable.Map(),
      targetAtomUri: jsonldConnectionImm.get("targetAtom"),
      targetConnectionUri: jsonldConnectionImm.get("targetConnection"),
      creationDate: undefined,
      lastUpdateDate: undefined,
      unread: undefined,
      isRated: false,
      showAgreementData: false,
      showPetriNetData: false,
      multiSelectType: undefined,
    },
  };

  if (
    !parsedConnection.data.socketUri ||
    !parsedConnection.data.targetSocketUri
  ) {
    console.error(
      "Cant parse connection, at least one of the mandatory socketUris is empty: ",
      jsonldConnectionImm.toJS()
    );
  } else if (
    !parsedConnection.data.uri ||
    !parsedConnection.belongsToUri ||
    !parsedConnection.data.targetAtomUri
  ) {
    console.error(
      "Cant parse connection, data is an invalid connection-object (mandatory uris could not be retrieved): ",
      jsonldConnectionImm.toJS()
    );
  } else if (
    !(
      parsedConnection.data.state === won.WON.RequestReceived ||
      parsedConnection.data.state === won.WON.RequestSent ||
      parsedConnection.data.state === won.WON.Suggested ||
      parsedConnection.data.state === won.WON.Connected ||
      parsedConnection.data.state === won.WON.Closed
    )
  ) {
    console.error(
      "Cant parse connection, data is an invalid connection-object (faulty state): ",
      jsonldConnectionImm.toJS()
    );
  } else {
    parsedConnection.data.unread = !isUriRead(parsedConnection.data.uri);

    const creationDate = jsonldConnectionImm.get("modified");
    if (creationDate) {
      parsedConnection.data.creationDate = new Date(creationDate);
      parsedConnection.data.lastUpdateDate = parsedConnection.data.creationDate;
    }

    return Immutable.fromJS(parsedConnection);
  }
  return undefined;
}
