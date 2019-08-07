/**
 * Component for rendering the icon of a groupChat (renders participants icons)
 * Can be included with either:
 *    connection-uri: then the participants of the targetAtom are shown
 *    atom-uri: then the participants of the atom behind the atom uri are shown
 * Created by quasarchimaere on 15.01.2019.
 */
import React from "react";
import {labels} from "../won-label-utils.js";
import {get, getIn} from "../utils.js";
import {actionCreators} from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as connectionUtils from "../redux/utils/connection-utils.js";

import won from "../won-es6";

export default class WonConnectionState extends React.Component {
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
    const atom = generalSelectors.getOwnedAtomByConnectionUri(state, this.connectionUri);
    const connection = getIn(atom, ["connections" , this.connectionUri]);;

    return {
      connectionState: get(connection, "state"),
      unread: connectionUtils.isUnread(connection)
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div/>;
    }

    let icon;

    switch (this.state.connectionState) {
      case won.WON.Suggested:
        icon = (<use xlinkHref="#ico36_match" href="#ico36_match"/>);
        break;
      case won.WON.RequestSent:
        icon = (<use xlinkHref="#ico36_outgoing" href="#ico36_outgoing"/>);
        break;
      case won.WON.RequestReceived:
        icon = (<use xlinkHref="#ico36_incoming" href="#ico36_incoming"/>);
        break;
      case won.WON.Connected:
        icon = (<use xlinkHref="#ico36_message" href="#ico36_message"/>);
        break;
      case won.WON.Closed:
      default:
        icon = (<use xlinkHref="#ico36_close_circle" href="#ico36_close_circle"/>);
        break;
    }

    return (
      <won-connection-state>
        <div className="cs__state" title={labels.connectionState[this.state.connectionState]}>
          <svg className={"cs__state__icon " + (this.state.unread ? " won-unread " : "")} ng-if="self.unread && self.state === self.WON.Suggested">
            {icon}
          </svg>
        </div>
      </won-connection-state>
    );
  }
}
