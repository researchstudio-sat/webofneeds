import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { get } from "../utils.js";

import * as accountUtils from "../redux/utils/account-utils.js";

import { actionCreators } from "../actions/actions.js";
import {
  getOwnedAtomByConnectionUri,
  getOwnedCondensedPersonaList,
} from "../redux/selectors/general-selectors.js";
import { getMessagesByConnectionUri } from "../redux/selectors/message-selectors.js";
import {
  isMessageAcceptable,
  isMessageCancelable,
  isMessageClaimable,
  isMessageProposable,
  isMessageRejectable,
  isMessageRetractable,
  isMessageSelected,
} from "../redux/utils/message-utils.js";
import * as connectionSelectors from "../redux/selectors/connection-selectors.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";

import "~/style/_chattextfield.scss";
import "~/style/_textfield.scss";

const mapStateToProps = (state, ownProps) => {
  //ownProps/ng-attributes from chat-textfield.js;
  /*
    onInput: "&",
    onPaste: "&",
    onSubmit: "&",
  */
  //************

  const post =
    ownProps.connectionUri &&
    getOwnedAtomByConnectionUri(state, ownProps.connectionUri);
  const connection =
    post && post.getIn(["connections", ownProps.connectionUri]);

  const messages = getMessagesByConnectionUri(state, ownProps.connectionUri);

  const selectedMessages =
    messages && messages.filter(msg => isMessageSelected(msg));
  const rejectableMessages =
    messages && messages.filter(msg => isMessageRejectable(msg));
  const retractableMessages =
    messages && messages.filter(msg => isMessageRetractable(msg));
  const acceptableMessages =
    messages && messages.filter(msg => isMessageAcceptable(msg));
  const proposableMessages =
    messages && messages.filter(msg => isMessageProposable(msg));
  const cancelableMessages =
    messages && messages.filter(msg => isMessageCancelable(msg));
  const claimableMessages =
    messages && messages.filter(msg => isMessageClaimable(msg));

  const hasRejectableMessages =
    rejectableMessages && rejectableMessages.size > 0;
  const hasRetractableMessages =
    retractableMessages && retractableMessages.size > 0;

  const hasAcceptableMessages =
    acceptableMessages && acceptableMessages.size > 0;
  const hasProposableMessages =
    proposableMessages && proposableMessages.size > 0;
  const hasCancelableMessages =
    cancelableMessages && cancelableMessages.size > 0;
  const hasClaimableMessages = claimableMessages && claimableMessages.size > 0;

  const selectedDetailIdentifier = state.getIn([
    "view",
    "selectedAddMessageContent",
  ]);
  const selectedDetail =
    this.allMessageDetails &&
    selectedDetailIdentifier &&
    this.allMessageDetails[selectedDetailIdentifier];
  return {
    connectionUri: ownProps.connectionUri,
    placeholder: ownProps.placeholder,
    helpText: ownProps.helpText,
    isCode: ownProps.isCode,
    allowDetails: ownProps.allowDetails,
    allowEmptySubmit: ownProps.allowEmptySubmit,
    showPersonas: ownProps.showPersonas,
    post,
    multiSelectType: connection && connection.get("multiSelectType"),
    showAgreementData: connection && connection.get("showAgreementData"),
    isChatToGroupConnection: connectionSelectors.isChatToGroupConnection(
      get(state, "atoms"),
      connection
    ),
    isConnected: connectionUtils.isConnected(connection),
    selectedMessages: selectedMessages,
    hasClaimableMessages,
    hasProposableMessages,
    hasCancelableMessages,
    hasAcceptableMessages,
    hasRetractableMessages,
    hasRejectableMessages,
    connectionHasBeenLost:
      state.getIn(["messages", "reconnecting"]) ||
      state.getIn(["messages", "lostConnection"]),
    showAddMessageContent: state.getIn(["view", "showAddMessageContent"]),
    selectedDetail,
    selectedDetailComponent: selectedDetail && selectedDetail.component,
    isLoggedIn: accountUtils.isLoggedIn(get(state, "account")),
    personas: getOwnedCondensedPersonaList(state).toJS(),
  };
};

const mapDispatchToProps = dispatch => {
  //TODO
  return {
    fetchUnloadedAtom: atomUri => {
      dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
    },
  };
};

class ChatTextfield extends React.Component {
  //TODO
  render() {
    return <chat-textfield />;
  }
}

ChatTextfield.propTypes = {
  connectionUri: PropTypes.string,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ChatTextfield);
