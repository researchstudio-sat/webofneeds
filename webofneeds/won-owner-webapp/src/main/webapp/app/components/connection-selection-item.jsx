import React from "react";
import PropTypes from "prop-types";
import { get, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";
import WonConnectionHeader from "./connection-header.jsx";

import "~/style/_connection-selection-item-line.scss";

export default class WonConnectionSelectionItem extends React.Component {
  static propTypes = {
    connectionUri: PropTypes.string.isRequired,
    ngRedux: PropTypes.object.isRequired,
    onClick: PropTypes.func,
  };

  componentDidMount() {
    this.connectionUri = this.props.connectionUri;
    this.disconnect = this.props.ngRedux.connect(
      this.selectFromState.bind(this),
      actionCreators
    )(state => {
      this.setState(state);
    });
  }

  componentWillUnmount() {
    this.disconnect();
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.connectionUri = nextProps.connectionUri;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const ownedAtom = generalSelectors.getOwnedAtomByConnectionUri(
      state,
      this.connectionUri
    );
    const connection = getIn(ownedAtom, ["connections", this.connectionUri]);
    const targetAtomUri = get(connection, "targetAtomUri");
    const processState = get(state, "process");
    return {
      openConnectionUri: generalSelectors.getConnectionUriFromRoute(state),
      lastUpdateTimestamp: get(connection, "lastUpdateDate"),
      targetAtomFailedToLoad: processUtils.hasAtomFailedToLoad(
        processState,
        targetAtomUri
      ),
      isUnread: connectionUtils.isUnread(connection),
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    const closeButton = this.state.targetAtomFailedToLoad ? (
      <button
        className="csi__closebutton red won-button--outlined thin"
        onClick="() => this.closeConnection()"
      >
        Close
      </button>
    ) : (
      undefined
    );

    return (
      <won-connection-selection-item
        class={
          (this.state.openConnectionUri === this.connectionUri
            ? "selected "
            : "") + (this.state.isUnread ? "won-unread" : "")
        }
      >
        <WonConnectionHeader
          connectionUri={this.connectionUri}
          ngRedux={this.props.ngRedux}
          onClick={this.props.onClick}
        />
        {closeButton}
      </won-connection-selection-item>
    );
  }

  closeConnection() {
    this.props.ngRedux.dispatch(
      actionCreators.connections__close(this.connectionUri)
    );
    this.props.ngRedux.dispatch(
      actionCreators.router__stateGoCurrent({
        useCase: undefined,
        connectionUri: undefined,
      })
    );
  }
}
