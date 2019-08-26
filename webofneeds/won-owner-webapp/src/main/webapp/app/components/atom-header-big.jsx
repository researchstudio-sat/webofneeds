/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { actionCreators } from "../actions/actions.js";

import "~/style/_atom-header-big.scss";
import * as atomUtils from "../redux/utils/atom-utils";
import { get, getIn } from "../utils.js";

import WonAtomContextDropdown from "../components/atom-context-dropdown.jsx";
import WonAtomIcon from "../components/atom-icon.jsx";
import WonShareDropdown from "../components/share-dropdown.jsx";

export default class WonAtomHeaderBig extends React.Component {
  componentDidMount() {
    this.atomUri = this.props.atomUri;
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
    this.atomUri = nextProps.atomUri;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const atom = state.getIn(["atoms", this.atomUri]);

    const personaUri = atomUtils.getHeldByUri(atom);
    const persona = getIn(state, ["atoms", personaUri]);
    const personaName = get(persona, "humanReadable");
    const isDirectResponse = atomUtils.isDirectResponseAtom(atom);
    const responseToUri =
      isDirectResponse && getIn(atom, ["content", "responseToUri"]);
    const responseToAtom =
      responseToUri && getIn(state, ["atoms", responseToUri]);

    return {
      atom,
      personaName,
      isDirectResponse,
      responseToAtom,
      isGroupChatEnabled: atomUtils.hasGroupSocket(atom),
      isChatEnabled: atomUtils.hasChatSocket(atom),
      atomTypeLabel: atom && atomUtils.generateTypeLabel(atom),
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    let titleElement;

    if (this.hasTitle()) {
      titleElement = <h1 className="ahb__title">{this.generateTitle()}</h1>;
    } else if (this.state.isDirectResponse) {
      titleElement = (
        <h1 className="ahb__title ahb__title--notitle">RE: no title</h1>
      );
    } else {
      titleElement = (
        <h1 className="ahb__title ahb__title--notitle">no title</h1>
      );
    }

    const personaNameElement = this.state.personaName && (
      <span className="ahb__titles__persona">{this.state.personaName}</span>
    );

    const groupChatElement = this.state.isGroupChatEnabled && (
      <span className="ahb__titles__groupchat">
        {"Group Chat" + (this.state.isChatEnabled ? " enabled" : "")}
      </span>
    );

    return (
      <won-atom-header-big>
        <nav className="atom-header-big">
          <div className="ahb__inner">
            <WonAtomIcon atomUri={this.atomUri} />
            <hgroup>
              {titleElement}

              {personaNameElement}
              {groupChatElement}
              <div className="ahb__titles__type">
                {this.state.atomTypeLabel}
              </div>
            </hgroup>
          </div>
          <WonShareDropdown atomUri={this.atomUri} />
          <WonAtomContextDropdown
            atomUri={this.atomUri}
            ngRedux={this.props.ngRedux}
          />
        </nav>
      </won-atom-header-big>
    );
  }

  generateTitle() {
    if (this.state.isDirectResponse && this.state.responseToAtom) {
      return "Re: " + get(this.state.responseToAtom, "humanReadable");
    } else {
      return get(this.state.atom, "humanReadable");
    }
  }

  hasTitle() {
    if (this.state.isDirectResponse && this.state.responseToAtom) {
      return !!get(this.state.responseToAtom, "humanReadable");
    } else {
      return !!get(this.state.atom, "humanReadable");
    }
  }
}

WonAtomHeaderBig.propTypes = {
  atomUri: PropTypes.string.isRequired,
  ngRedux: PropTypes.object.isRequired,
};
