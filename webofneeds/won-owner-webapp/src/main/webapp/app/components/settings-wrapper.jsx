import "./identicon.js";
import "~/style/_won-markdown.scss";
import React from "react";
// import PropTypes from "prop-types";
import ElmComponent from "react-elm-components";
import { actionCreators } from "../actions/actions.js";
import { ReactReduxContext } from "react-redux";
import * as accountUtils from "../redux/utils/account-utils.js";
import { get } from "../utils";
import { Elm } from "../../elm/Settings.elm";

import {
  getOwnedCondensedPersonaList,
  currentSkin,
} from "../redux/selectors/general-selectors.js";

export default class WonSettingsWrapper extends React.Component {
  componentWillUnmount() {
    if (this.state && this.state.ports) {
      this.state.ports.personaOut.unsubscribe();
      this.state.ports.updatePersonas.unsubscribe();
      this.state.ports.getVerified.unsubscribe();
      this.state.ports.getAccountInfo.unsubscribe();

      // this.state.ports.inPort.send({
      //   unmount: true,
      // });
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

  // UNSAFE_componentWillReceiveProps(nextProps) {
  // if (this.state && this.state.ports && nextProps.flags) {
  //   this.state.ports.inPort.send({
  //     newProps: nextProps.flags,
  //   });
  // }
  // }

  setupPorts(ports) {
    const dispatch = this.context.store.dispatch;
    const getState = this.context.store.getState;

    ports.personaOut.subscribe(persona => {
      dispatch(actionCreators.personas__create(persona));
    });

    ports.updatePersonas.subscribe(() => {
      const personas = getOwnedCondensedPersonaList(getState(), true);
      if (personas) {
        ports.personaIn.send(personas.toJS());
      }
    });

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

    this.setState({ ports });
  }
}
WonSettingsWrapper.propTypes = {
  // src: PropTypes.object.isRequired,
  // flags: PropTypes.object,
};
WonSettingsWrapper.contextType = ReactReduxContext;
