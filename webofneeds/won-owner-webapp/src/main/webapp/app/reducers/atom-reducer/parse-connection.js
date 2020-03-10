import Immutable from "immutable";
import vocab from "../../service/vocab.js";
import { get } from "../../utils.js";
import { isUriRead } from "../../won-localstorage.js";

export function parseMetaConnection(metaConnection) {
  const metaConnectionImm = Immutable.fromJS(metaConnection);

  let parsedMetaConnection = {
    belongsToUri: get(metaConnectionImm, "sourceAtom"),
    data: {
      uri: get(metaConnectionImm, "uri"),
      state: get(metaConnectionImm, "connectionState"),
      messages: Immutable.Map(),
      socketUri: get(metaConnectionImm, "socket"),
      targetSocketUri: get(metaConnectionImm, "targetSocket"),
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
      targetAtomUri: get(metaConnectionImm, "targetAtom"),
      targetConnectionUri: get(metaConnectionImm, "targetConnection"),
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
    !parsedMetaConnection.data.socketUri ||
    !parsedMetaConnection.data.targetSocketUri
  ) {
    console.error(
      "Cant parse connection, at least one of the mandatory socketUris is empty: ",
      metaConnection.toJS()
    );
  } else if (
    !parsedMetaConnection.data.uri ||
    !parsedMetaConnection.belongsToUri ||
    !parsedMetaConnection.data.targetAtomUri
  ) {
    console.error(
      "Cant parse connection, data is an invalid connection-object (mandatory uris could not be retrieved): ",
      metaConnection.toJS()
    );
  } else if (
    !(
      parsedMetaConnection.data.state === vocab.WON.RequestReceived ||
      parsedMetaConnection.data.state === vocab.WON.RequestSent ||
      parsedMetaConnection.data.state === vocab.WON.Suggested ||
      parsedMetaConnection.data.state === vocab.WON.Connected ||
      parsedMetaConnection.data.state === vocab.WON.Closed
    )
  ) {
    console.error(
      "Cant parse connection, data is an invalid connection-object (faulty state): ",
      metaConnection.toJS()
    );
  } else {
    parsedMetaConnection.data.unread = !isUriRead(
      parsedMetaConnection.data.uri
    );

    const creationDate = metaConnection.get("modified");

    if (creationDate) {
      parsedMetaConnection.data.creationDate = new Date(creationDate);
      parsedMetaConnection.data.lastUpdateDate =
        parsedMetaConnection.data.creationDate;
    }

    return Immutable.fromJS(parsedMetaConnection);
  }
  return undefined;
}

export function parseConnection(jsonldConnection) {
  const jsonldConnectionImm = Immutable.fromJS(jsonldConnection);

  let parsedConnection = {
    belongsToUri: jsonldConnectionImm.get("sourceAtom"),
    data: {
      uri: get(jsonldConnectionImm, "uri"),
      state: get(jsonldConnectionImm, "connectionState"),
      previousState: get(jsonldConnectionImm, "previousConnectionState"),
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

    const creationDate = jsonldConnectionImm.get("modified");
    if (creationDate) {
      parsedConnection.data.creationDate = new Date(creationDate);
      parsedConnection.data.lastUpdateDate = parsedConnection.data.creationDate;
    }

    return Immutable.fromJS(parsedConnection);
  }
  return undefined;
}
