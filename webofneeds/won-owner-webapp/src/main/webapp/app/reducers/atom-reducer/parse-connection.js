import Immutable from "immutable";
import vocab from "../../service/vocab.js";
import { get } from "../../utils.js";
import { isUriRead } from "../../won-localstorage.js";

export function parseConnection(jsonldConnection) {
  const jsonldConnectionImm = Immutable.fromJS(jsonldConnection);

  let parsedConnection = {
    belongsToUri: get(jsonldConnectionImm, "sourceAtom"),
    data: {
      uri: get(jsonldConnectionImm, "uri"),
      state: get(jsonldConnectionImm, "connectionState"),
      previousState: get(jsonldConnectionImm, "previousConnectionState"),
      messageContainerUri: get(jsonldConnectionImm, "messageContainer"),
      messages: Immutable.Map(),
      socketUri: get(jsonldConnectionImm, "socket"),
      targetSocketUri: get(jsonldConnectionImm, "targetSocket"),
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
      targetAtomUri: get(jsonldConnectionImm, "targetAtom"),
      targetConnectionUri: get(jsonldConnectionImm, "targetConnection"),
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
      parsedConnection.data.state === vocab.WON.RequestReceived ||
      parsedConnection.data.state === vocab.WON.RequestSent ||
      parsedConnection.data.state === vocab.WON.Suggested ||
      parsedConnection.data.state === vocab.WON.Connected ||
      parsedConnection.data.state === vocab.WON.Closed
    )
  ) {
    console.error(
      "Cant parse connection, data is an invalid connection-object (faulty state): ",
      jsonldConnectionImm.toJS()
    );
  } else {
    parsedConnection.data.unread = !isUriRead(parsedConnection.data.uri);

    const creationDate = get(jsonldConnectionImm, "modified");
    if (creationDate) {
      parsedConnection.data.creationDate = new Date(creationDate);
      parsedConnection.data.lastUpdateDate = parsedConnection.data.creationDate;
    }

    return Immutable.fromJS(parsedConnection);
  }
  return undefined;
}
