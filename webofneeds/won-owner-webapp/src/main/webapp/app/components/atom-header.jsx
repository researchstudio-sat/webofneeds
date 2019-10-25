/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { get, getIn } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import { relativeTime } from "../won-label-utils.js";
import { selectLastUpdateTime } from "../redux/selectors/general-selectors.js";

import * as processUtils from "../redux/utils/process-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";

import "~/style/_atom-header.scss";
import WonAtomIcon from "./atom-icon.jsx";
import VisibilitySensor from "react-visibility-sensor";
import PropTypes from "prop-types";
import { connect } from "react-redux";

const mapStateToProps = (state, ownProps) => {
  const atom = getIn(state, ["atoms", ownProps.atomUri]);
  const isDirectResponse = atomUtils.isDirectResponseAtom(atom);
  const responseToUri =
    isDirectResponse && getIn(atom, ["content", "responseToUri"]);
  const responseToAtom = responseToUri
    ? getIn(state, ["atoms", responseToUri])
    : undefined;

  const personaUri = atomUtils.getHeldByUri(atom);
  const persona = personaUri && getIn(state, ["atoms", personaUri]);
  const personaName = get(persona, "humanReadable");

  const process = get(state, "process");

  return {
    atomUri: ownProps.atomUri,
    onClick: ownProps.onClick,
    responseToAtom,
    atom,
    atomTypeLabel: atom && atomUtils.generateTypeLabel(atom),
    personaName,
    atomLoading: !atom || processUtils.isAtomLoading(process, ownProps.atomUri),
    atomToLoad: !atom || processUtils.isAtomToLoad(process, ownProps.atomUri),
    atomFailedToLoad:
      atom && processUtils.hasAtomFailedToLoad(process, ownProps.atomUri),
    isDirectResponse: isDirectResponse,
    isGroupChatEnabled: atomUtils.hasGroupSocket(atom),
    isChatEnabled: atomUtils.hasChatSocket(atom),
    hideTimestamp: ownProps.hideTimestamp,
    friendlyTimestamp:
      !ownProps.hideTimestamp &&
      atom &&
      relativeTime(selectLastUpdateTime(state), get(atom, "lastUpdateDate")),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    fetchAtom: uri => {
      dispatch(actionCreators.atoms__fetchUnloadedAtom(uri));
    },
  };
};

class WonAtomHeader extends React.Component {
  render() {
    let atomHeaderContent;
    let atomHeaderIcon;

    if (this.atomLoading) {
      //Loading View

      atomHeaderIcon = <div className="ph__icon__skeleton" />;
      atomHeaderContent = (
        <div className="ph__right">
          <div className="ph__right__topline">
            <div className="ph__right__topline__title" />
          </div>
          <div className="ph__right__subtitle">
            <span className="ph__right__subtitle__type" />
          </div>
        </div>
      );
    } else if (get(this.props.atom, "isBeingCreated")) {
      //In Creation View
      atomHeaderIcon = <WonAtomIcon atomUri={this.props.atomUri} />;
      atomHeaderContent = (
        <div className="ph__right">
          <div className="ph__right__topline">
            <div className="ph__right__topline__notitle">Creating...</div>
          </div>
          <div className="ph__right__subtitle">
            <span className="ph__right__subtitle__type">
              {this.props.personaName ? (
                <span className="ph__right__subtitle__type__persona">
                  {this.props.personaName}
                </span>
              ) : (
                undefined
              )}
              {this.props.isGroupChatEnabled ? (
                <span className="ph__right__subtitle__type__groupchat">
                  {this.props.isChatEnabled
                    ? "Group Chat enabled"
                    : "Group Chat"}
                </span>
              ) : (
                undefined
              )}
              <span className="ph__right__subtitle__type">
                {this.props.atomTypeLabel}
              </span>
            </span>
          </div>
        </div>
      );
    } else if (this.props.atomFailedToLoad) {
      //FailedToLoad View
      atomHeaderIcon = <WonAtomIcon atomUri={this.props.atomUri} />;
      atomHeaderContent = (
        <div className="ph__right">
          <div className="ph__right__topline">
            <div className="ph__right__topline__notitle">
              Atom Loading failed
            </div>
          </div>
          <div className="ph__right__subtitle">
            <span className="ph__right__subtitle__type">
              Atom might have been deleted.
            </span>
          </div>
        </div>
      );
    } else {
      //Normal View
      atomHeaderIcon = <WonAtomIcon atomUri={this.props.atomUri} />;
      atomHeaderContent = (
        <div className="ph__right">
          <div className="ph__right__topline">
            <div className="ph__right__topline__title">
              {this.hasTitle()
                ? this.generateTitle()
                : this.props.isDirectResponse
                  ? "RE: no title"
                  : "no title"}
            </div>
          </div>
          <div className="ph__right__subtitle">
            <span className="ph__right__subtitle__type">
              {this.props.personaName ? (
                <span className="ph__right__subtitle__type__persona">
                  {this.props.personaName}
                </span>
              ) : (
                undefined
              )}
              {this.props.isGroupChatEnabled ? (
                <span className="ph__right__subtitle__type__groupchat">
                  {this.props.isChatEnabled
                    ? "Group Chat enabled"
                    : "Group Chat"}
                </span>
              ) : (
                undefined
              )}
              <span>{this.props.atomTypeLabel}</span>
            </span>
            {!this.props.hideTimestamp && (
              <div className="ph__right__subtitle__date">
                {this.props.friendlyTimestamp}
              </div>
            )}
          </div>
        </div>
      );
    }

    return (
      <won-atom-header
        class={
          (this.props.atomLoading ? " won-is-loading " : "") +
          (this.props.atomToLoad ? " won-to-load " : "") +
          (this.props.onClick ? " clickable " : "")
        }
        onClick={this.props.onClick}
      >
        {atomHeaderIcon}
        <VisibilitySensor
          onChange={isVisible => {
            this.onChange(isVisible);
          }}
          intervalDelay={200}
          partialVisibility={true}
          offset={{ top: -300, bottom: -300 }}
        >
          {atomHeaderContent}
        </VisibilitySensor>
      </won-atom-header>
    );
  }

  ensureAtomIsLoaded() {
    if (
      this.props.atomUri &&
      (!this.props.atom || (this.props.atomToLoad && !this.props.atomLoading))
    ) {
      this.props.fetchAtom(this.props.atomUri);
    }
  }

  hasTitle() {
    if (this.props.isDirectResponse && this.props.responseToAtom) {
      return !!get(this.props.responseToAtom, "humanReadable");
    } else {
      return !!get(this.props.atom, "humanReadable");
    }
  }

  generateTitle() {
    if (this.isDirectResponse && this.responseToAtom) {
      return "Re: " + get(this.props.responseToAtom, "humanReadable");
    } else {
      return get(this.props.atom, "humanReadable");
    }
  }

  onChange(isVisible) {
    if (isVisible) {
      this.ensureAtomIsLoaded();
    }
  }
}

WonAtomHeader.propTypes = {
  atomUri: PropTypes.string.isRequired,
  hideTimestamp: PropTypes.bool,
  onClick: PropTypes.func,
  fetchAtom: PropTypes.func,
  responseToAtom: PropTypes.object,
  atom: PropTypes.object,
  atomTypeLabel: PropTypes.string,
  personaName: PropTypes.string,
  atomLoading: PropTypes.bool,
  atomToLoad: PropTypes.bool,
  atomFailedToLoad: PropTypes.bool,
  isDirectResponse: PropTypes.bool,
  isGroupChatEnabled: PropTypes.bool,
  isChatEnabled: PropTypes.bool,
  friendlyTimestamp: PropTypes.any,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonAtomHeader);
