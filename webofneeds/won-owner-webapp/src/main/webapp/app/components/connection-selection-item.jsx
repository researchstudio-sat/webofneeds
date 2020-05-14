import React from "react";
import PropTypes from "prop-types";
import { get, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { connect } from "react-redux";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import WonConnectionHeader from "./connection-header.jsx";

import "~/style/_connection-selection-item-line.scss";
import { generateLink, getQueryParams } from "../utils";
import { withRouter } from "react-router-dom";

const mapStateToProps = (state, ownProps) => {
  const { connectionUri } = getQueryParams(ownProps.location);
  const openConnectionUri = connectionUri;

  const ownedAtom = generalSelectors.getOwnedAtomByConnectionUri(
    state,
    ownProps.connectionUri
  );
  const connection = getIn(ownedAtom, ["connections", ownProps.connectionUri]);
  const targetAtomUri = get(connection, "targetAtomUri");
  const processState = get(state, "process");
  return {
    connectionUri: ownProps.connectionUri,
    openConnectionUri: openConnectionUri,
    lastUpdateTimestamp: get(connection, "lastUpdateDate"),
    targetAtomFailedToLoad: processUtils.hasAtomFailedToLoad(
      processState,
      targetAtomUri
    ),
    isUnread: connectionUtils.isUnread(connection),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    connectionClose: connectionUri => {
      dispatch(actionCreators.connections__close(connectionUri));
    },
  };
};

class WonConnectionSelectionItem extends React.Component {
  constructor(props) {
    super(props);
    this.closeConnection = this.closeConnection.bind(this);
  }

  render() {
    const closeButton = this.props.targetAtomFailedToLoad ? (
      <button
        className="csi__closebutton red won-button--outlined thin"
        onClick={this.closeConnection}
      >
        Close
      </button>
    ) : (
      undefined
    );

    return (
      <won-connection-selection-item
        class={
          (this.props.openConnectionUri === this.props.connectionUri
            ? "selected "
            : "") + (this.props.isUnread ? "won-unread" : "")
        }
      >
        <WonConnectionHeader
          connectionUri={this.props.connectionUri}
          toLink={this.props.toLink}
        />
        {closeButton}
      </won-connection-selection-item>
    );
  }

  closeConnection() {
    this.props.connectionClose(this.props.connectionUri);
    this.props.history.push(
      generateLink(this.props.history.location, {
        useCase: undefined,
        connectionUri: undefined,
      })
    );
  }
}

WonConnectionSelectionItem.propTypes = {
  connectionUri: PropTypes.string.isRequired,
  toLink: PropTypes.string,
  openConnectionUri: PropTypes.string,
  lastUpdateTimestamp: PropTypes.any,
  targetAtomFailedToLoad: PropTypes.bool,
  isUnread: PropTypes.bool,
  connectionClose: PropTypes.func,
  history: PropTypes.object,
};

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(WonConnectionSelectionItem)
);
