import Immutable from "immutable";
import won from "../../won-es6.js";
import { get } from "../../utils.js";
import { isUriRead } from "../../won-localstorage.js";

export function parseMetaConnection(metaConnection) {
  const metaConnectionImm = Immutable.fromJS(metaConnection);

  let parsedMetaConnection = {
    belongsToUri: get(metaConnectionImm, "atomUri"),
    data: {
      uri: get(metaConnectionImm, "connectionUri"),
      state: get(metaConnectionImm, "connectionState"),
      messages: Immutable.Map(),
      socketUri: get(metaConnectionImm, "socketUri"),
      targetSocketUri: get(metaConnectionImm, "targetSocketUri"),
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
      targetAtomUri: get(metaConnectionImm, "targetAtomUri"),
      targetConnectionUri: get(metaConnectionImm, "targetConnectionUri"),
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
      parsedMetaConnection.data.state === won.WON.RequestReceived ||
      parsedMetaConnection.data.state === won.WON.RequestSent ||
      parsedMetaConnection.data.state === won.WON.Suggested ||
      parsedMetaConnection.data.state === won.WON.Connected ||
      parsedMetaConnection.data.state === won.WON.Closed
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
