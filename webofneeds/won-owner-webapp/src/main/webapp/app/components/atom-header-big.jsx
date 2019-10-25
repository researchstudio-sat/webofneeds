/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";

import "~/style/_atom-header-big.scss";
import * as atomUtils from "../redux/utils/atom-utils";
import * as accountUtils from "../redux/utils/account-utils";
import { get, getIn } from "../utils.js";

import WonAtomContextDropdown from "../components/atom-context-dropdown.jsx";
import WonAtomIcon from "../components/atom-icon.jsx";
import WonShareDropdown from "../components/share-dropdown.jsx";
import WonAddBuddy from "../components/add-buddy.jsx";

const mapStateToProps = (state, ownProps) => {
  const atom = state.getIn(["atoms", ownProps.atomUri]);

  const personaUri = atomUtils.getHeldByUri(atom);
  const persona = getIn(state, ["atoms", personaUri]);
  const personaName = get(persona, "humanReadable");
  const isDirectResponse = atomUtils.isDirectResponseAtom(atom);
  const responseToUri =
    isDirectResponse && getIn(atom, ["content", "responseToUri"]);
  const responseToAtom = responseToUri
    ? getIn(state, ["atoms", responseToUri])
    : undefined;

  const atomUri = ownProps.atomUri;
  const accountState = get(state, "account");

  return {
    atomUri,
    atom,
    personaName,
    isDirectResponse,
    responseToAtom,
    isGroupChatEnabled: atomUtils.hasGroupSocket(atom),
    isChatEnabled: atomUtils.hasChatSocket(atom),
    showAddBuddyElement:
      atomUtils.hasBuddySocket(atom) &&
      !accountUtils.isAtomOwned(accountState, atomUri),
    atomTypeLabel: atom && atomUtils.generateTypeLabel(atom),
  };
};

class WonAtomHeaderBig extends React.Component {
  render() {
    let titleElement;

    if (this.hasTitle()) {
      titleElement = <h1 className="ahb__title">{this.generateTitle()}</h1>;
    } else if (this.props.isDirectResponse) {
      titleElement = (
        <h1 className="ahb__title ahb__title--notitle">RE: no title</h1>
      );
    } else {
      titleElement = (
        <h1 className="ahb__title ahb__title--notitle">no title</h1>
      );
    }

    const personaNameElement = this.props.personaName && (
      <span className="ahb__titles__persona">{this.props.personaName}</span>
    );

    const groupChatElement = this.props.isGroupChatEnabled && (
      <span className="ahb__titles__groupchat">
        {"Group Chat" + (this.props.isChatEnabled ? " enabled" : "")}
      </span>
    );

    const buddyActionElement = this.props.showAddBuddyElement && (
      <WonAddBuddy atomUri={this.props.atomUri} />
    );

    return (
      <won-atom-header-big>
        <nav className="atom-header-big">
          <div className="ahb__inner">
            <WonAtomIcon atomUri={this.props.atomUri} />
            <hgroup>
              {titleElement}

              {personaNameElement}
              {groupChatElement}
              <div className="ahb__titles__type">
                {this.props.atomTypeLabel}
              </div>
            </hgroup>
          </div>
          {buddyActionElement}
          <WonShareDropdown atomUri={this.props.atomUri} />
          <WonAtomContextDropdown atomUri={this.props.atomUri} />
        </nav>
      </won-atom-header-big>
    );
  }

  generateTitle() {
    if (this.props.isDirectResponse && this.props.responseToAtom) {
      return "Re: " + get(this.props.responseToAtom, "humanReadable");
    } else {
      return get(this.props.atom, "humanReadable");
    }
  }

  hasTitle() {
    if (this.props.isDirectResponse && this.props.responseToAtom) {
      return !!get(this.props.responseToAtom, "humanReadable");
    } else {
      return !!get(this.props.atom, "humanReadable");
    }
  }
}

WonAtomHeaderBig.propTypes = {
  atomUri: PropTypes.string.isRequired,
  atom: PropTypes.object,
  personaName: PropTypes.string,
  isDirectResponse: PropTypes.bool,
  responseToAtom: PropTypes.object,
  isGroupChatEnabled: PropTypes.bool,
  isChatEnabled: PropTypes.bool,
  showAddBuddyElement: PropTypes.bool,
  atomTypeLabel: PropTypes.string,
};

export default connect(mapStateToProps)(WonAtomHeaderBig);
