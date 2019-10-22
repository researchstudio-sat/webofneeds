import "./identicon.js";
import "~/style/_won-markdown.scss";
import React from "react";
// import PropTypes from "prop-types";
import ElmComponent from "react-elm-components";
import { ReactReduxContext } from "react-redux";
import * as accountUtils from "../redux/utils/account-utils.js";
import { get } from "../utils";
import { Elm } from "../../elm/Settings.elm";

import { currentSkin } from "../redux/selectors/general-selectors.js";

class WonSettingsWrapper extends React.Component {
  componentWillUnmount() {
    if (this.state && this.state.disconnectPorts) {
      this.state.disconnectPorts();
    }
  }

  render() {
    return (
      <ElmComponent
        src={Elm.Settings}
        flags={{
          skin: currentSkin(),
          flags: { width: window.innerWidth, height: window.innerHeight },
        }}
        ports={this.setupPorts.bind(this)}
      />
    );
  }

  setupPorts(ports) {
    const getState = this.context.store.getState;
    const connect = this.context.store.connect;

    //// set up listeners on out-ports

    ports.getVerified.subscribe(() => {
      const isVerified = accountUtils.isEmailVerified(
        get(getState(), "account")
      );
      ports.isVerified.send(isVerified);
    });

    ports.getAccountInfo.subscribe(() => {
      const accountInfo = {
        email: accountUtils.getEmail(get(getState(), "account")),
        isVerified: accountUtils.isEmailVerified(get(getState(), "account")),
      };

      ports.accountInfoIn.send(accountInfo);
    });

    //// stream theme updates to the elm component

    const selectSkinFromState = state => ({
      skin: state.getIn(["config", "theme"]),
    });
    const skinPseudoComponent = () => {
      ports.skin.send(currentSkin());
    };
    const disconnectSkinPort = connect(selectSkinFromState)(
      skinPseudoComponent
    );

    //// attach ports reference to component for use elsewhere

    this.setState({
      ports: ports,
      disconnectPorts: () => {
        ports.getVerified.unsubscribe();
        ports.getAccountInfo.unsubscribe();
        disconnectSkinPort();
      },
    });
  }
}
WonSettingsWrapper.propTypes = {};
WonSettingsWrapper.contextType = ReactReduxContext;

export default WonSettingsWrapper;
