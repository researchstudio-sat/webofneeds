/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import {get, getIn} from "../utils.js";
import {actionCreators} from "../actions/actions.js";
import {relativeTime} from "../won-label-utils.js";
import {selectLastUpdateTime} from "../redux/selectors/general-selectors.js";

import * as processUtils from "../redux/utils/process-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";

import "~/style/_atom-header.scss";
import WonAtomIcon from "./atom-icon.jsx";
import VisibilitySensor from "react-visibility-sensor";

export default class WonAtomHeader extends React.Component {
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
    const atom = getIn(state, ["atoms", this.atomUri]);
    const isDirectResponse = atomUtils.isDirectResponseAtom(atom);
    const responseToUri =
      isDirectResponse && getIn(atom, ["content", "responseToUri"]);
    const responseToAtom =
      responseToUri && getIn(state, ["atoms", responseToUri]);

    const personaUri = atomUtils.getHeldByUri(atom);
    const persona = personaUri && getIn(state, ["atoms", personaUri]);
    const personaName = get(persona, "humanReadable");

    const process = get(state, "process");

    return {
      responseToAtom,
      atom,
      atomTypeLabel: atom && atomUtils.generateTypeLabel(atom),
      personaName,
      atomLoading:
        !atom || processUtils.isAtomLoading(process, this.atomUri),
      atomToLoad: !atom || processUtils.isAtomToLoad(process, this.atomUri),
      atomFailedToLoad:
        atom && processUtils.hasAtomFailedToLoad(process, this.atomUri),
      isDirectResponse: isDirectResponse,
      isGroupChatEnabled: atomUtils.hasGroupSocket(atom),
      isChatEnabled: atomUtils.hasChatSocket(atom),
      friendlyTimestamp:
        atom &&
        relativeTime(
          selectLastUpdateTime(state),
          get(atom, "lastUpdateDate")
        ),
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div/>;
    }

    let atomHeaderContent;
    let atomHeaderIcon;

    if (this.atomLoading) { //Loading View

      atomHeaderIcon = (<div className="ph__icon__skeleton"/>);
      atomHeaderContent = (
        <div className="ph__right">
          <div className="ph__right__topline">
            <div className="ph__right__topline__title"></div>
          </div>
          <div className="ph__right__subtitle">
            <span className="ph__right__subtitle__type"></span>
          </div>
        </div>
      );
    } else if(get(this.state.atom,"isBeingCreated")) { //In Creation View
      atomHeaderIcon = (<WonAtomIcon atomUri={this.atomUri} ngRedux={this.props.ngRedux}/>);
      atomHeaderContent = (
        <div className="ph__right">
          <div className="ph__right__topline">
            <div className="ph__right__topline__notitle">Creating...</div>
          </div>
          <div className="ph__right__subtitle">
            <span className="ph__right__subtitle__type">
              { this.state.personaName ? <span className="ph__right__subtitle__type__persona">{this.state.personaName}</span> : undefined }
              { this.state.isGroupChatEnabled ? <span className="ph__right__subtitle__type__groupchat">{this.state.isChatEnabled ? "Group Chat enabled" : "Group Chat"}</span> : undefined}
              <span className="ph__right__subtitle__type">{this.state.atomTypeLabel}</span>
            </span>
          </div>
        </div>
      );
    } else if(this.state.atomFailedToLoad) { //FailedToLoad View
      atomHeaderIcon = (<WonAtomIcon atomUri={this.atomUri} ngRedux={this.props.ngRedux}/>);
      atomHeaderContent = (
        <div className="ph__right">
          <div className="ph__right__topline">
            <div className="ph__right__topline__notitle">Atom Loading failed</div>
          </div>
          <div className="ph__right__subtitle">
            <span className="ph__right__subtitle__type">Atom might have been deleted.</span>
          </div>
        </div>
      );
    } else { //Normal View
      atomHeaderIcon = (<WonAtomIcon atomUri={this.atomUri} ngRedux={this.props.ngRedux}/>);
      atomHeaderContent = (
        <div className="ph__right">
          <div className="ph__right__topline">
            <div className="ph__right__topline__title">
              {
                this.hasTitle()
                ? this.generateTitle()
                : (
                  this.state.isDirectResponse
                  ? "RE: no title"
                  : "no title"
                )
              }
            </div>
          </div>
          <div className="ph__right__subtitle">
            <span className="ph__right__subtitle__type">
              { this.state.personaName ? <span className="ph__right__subtitle__type__persona">{this.state.personaName}</span> : undefined }
              { this.state.isGroupChatEnabled ? <span className="ph__right__subtitle__type__groupchat">{this.state.isChatEnabled ? "Group Chat enabled" : "Group Chat"}</span> : undefined}
              <span>{this.state.atomTypeLabel}</span>
            </span>
            <div className="ph__right__subtitle__date">{this.state.friendlyTimestamp}</div>
          </div>
        </div>
      );
    }

    return (

        <won-atom-header class={(this.state.atomLoading ? " won-is-loading " : "") + (this.state.atomToLoad ? " won-to-load " : "") + (this.props.onClick? " clickable " : "")} onClick={this.props.onClick}>
          {atomHeaderIcon}
          <VisibilitySensor onChange={(isVisible) => { this.onChange(isVisible) }} intervalDelay={200} partialVisibility={true} offset={{top: -300, bottom: -300}}>
            {atomHeaderContent}
          </VisibilitySensor>
        </won-atom-header>
    );
  }

  ensureAtomIsLoaded() {
    if (
      this.state.atomUri &&
      (!this.state.atom || (this.state.atomToLoad && !this.state.atomLoading))
    ) {
      this.props.ngRedux.dispatch(actionCreators.atoms__fetchUnloadedAtom(this.state.atomUri));
    }
  }

  hasTitle() {
    if (this.state.isDirectResponse && this.state.responseToAtom) {
      return !!get(this.state.responseToAtom, "humanReadable");
    } else {
      return !!get(this.state.atom, "humanReadable");
    }
  }

  generateTitle() {
    if (this.isDirectResponse && this.responseToAtom) {
      return "Re: " + get(this.state.responseToAtom, "humanReadable");
    } else {
      return get(this.state.atom, "humanReadable");
    }
  }

  onChange(isVisible) {
    if (isVisible) {
      this.ensureAtomIsLoaded();
    }
  }
}