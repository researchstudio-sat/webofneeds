import React from "react";
import PropTypes from "prop-types";
import { actionCreators } from "../actions/actions.js";
import { connect } from "react-redux";
import { get, getIn } from "../utils.js";

import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as viewUtils from "../redux/utils/view-utils.js";

import WonAtomContentHolds from "./atom-content-holds.jsx";
import WonAtomContentSuggestions from "./atom-content-suggestions.jsx";
import WonAtomContentBuddies from "./atom-content-buddies.jsx";
import WonAtomContentParticipants from "./atom-content-participants.jsx";
import WonAtomContentGeneral from "./atom-content-general.jsx";
import WonAtomContentPersona from "./atom-content-persona.jsx";
import WonAtomContentDetails from "./atom-content-details.jsx";
import WonLabelledHr from "./labelled-hr.jsx";
import WonTrig from "./trig.jsx";
import ElmReact from "./elm-react.jsx";
import { Elm } from "../../elm/AddPersona.elm";

import "~/style/_atom-content.scss";
import "~/style/_rdflink.scss";

const mapStateToProps = (state, ownProps) => {
  const openConnectionUri = generalSelectors.getConnectionUriFromRoute(state);
  const atom = getIn(state, ["atoms", ownProps.atomUri]);
  const isOwned = generalSelectors.isAtomOwned(state, ownProps.atomUri);
  const isActive = atomUtils.isActive(atom);
  const content = get(atom, "content");

  //TODO it will be possible to have more than one seeks
  const seeks = get(atom, "seeks");

  /**
   * This function checks if there is at least one detail present that is displayable
   */
  const hasVisibleDetails = contentBranchImm => {
    return !!(
      contentBranchImm &&
      contentBranchImm.find(
        (detailValue, detailKey) =>
          detailKey != "type" &&
          detailKey != "sockets" &&
          detailKey != "defaultSocket"
      )
    );
  };

  const hasContent = hasVisibleDetails(content);
  const hasSeeksBranch = hasVisibleDetails(seeks);

  const viewState = get(state, "view");
  const process = get(state, "process");

  return {
    atomUri: ownProps.atomUri,
    hasContent,
    hasSeeksBranch,
    atom,
    isOwned,
    isActive,
    isHeld: atomUtils.isHeld(atom),
    hasChatSocket: atomUtils.hasChatSocket(atom),
    hasHoldableSocket: atomUtils.hasHoldableSocket(atom),
    atomLoading: !atom || processUtils.isAtomLoading(process, ownProps.atomUri),
    atomFailedToLoad:
      atom && processUtils.hasAtomFailedToLoad(process, ownProps.atomUri),
    atomProcessingUpdate:
      atom && processUtils.isAtomProcessingUpdate(process, ownProps.atomUri),
    createdTimestamp: atom && atom.get("creationDate"),
    shouldShowRdf: viewUtils.showRdf(viewState),
    fromConnection: !!openConnectionUri,
    openConnectionUri,
    visibleTab: viewUtils.getVisibleTabByAtomUri(
      viewState,
      ownProps.atomUri,
      ownProps.defaultTab
    ),
    personas: generalSelectors.getOwnedCondensedPersonaList(state),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    fetchAtom: atomUri => {
      dispatch(actionCreators.atoms__fetchUnloadedAtom(atomUri));
    },
  };
};

class WonAtomContent extends React.Component {
  render() {
    if (this.props.atomLoading) {
      return (
        <won-atom-content class="won-is-loading">
          <div className="atom-skeleton">
            <h2 className="atom-skeleton__heading" />
            <p className="atom-skeleton__details" />
            <h2 className="atom-skeleton__heading" />
            <p className="atom-skeleton__details" />
            <h2 className="atom-skeleton__heading" />
            <p className="atom-skeleton__details" />
            <p className="atom-skeleton__details" />
            <p className="atom-skeleton__details" />
            <p className="atom-skeleton__details" />
            <p className="atom-skeleton__details" />
            <h2 className="atom-skeleton__heading" />
            <div className="atom-skeleton__details" />
          </div>
        </won-atom-content>
      );
    } else if (this.props.atomFailedToLoad) {
      return (
        <won-atom-content>
          <div className="atom-failedtoload">
            <svg className="atom-failedtoload__icon">
              <use
                xlinkHref="#ico16_indicator_error"
                href="#ico16_indicator_error"
              />
            </svg>
            <span className="atom-failedtoload__label">
              Failed To Load - Atom might have been deleted
            </span>
            <div className="atom-failedtoload__actions">
              <button
                className="atom-failedtoload__actions__button red won-button--outlined thin"
                onClick={() => this.tryReload()}
              >
                Try Reload
              </button>
            </div>
          </div>
        </won-atom-content>
      );
    } else {
      const processingUpdateElement = this.props.atomProcessingUpdate && (
        <div className="atom-content__updateindicator">
          <svg className="hspinner atom-content__updateindicator__spinner">
            <use xlinkHref="#ico_loading_anim" href="#ico_loading_anim" />
          </svg>
          <span className="atom-content__updateindicator__label">
            Processing changes...
          </span>
        </div>
      );

      let visibleTabFragment;
      if (this.isSelectedTab("DETAIL")) {
        visibleTabFragment = (
          <React.Fragment>
            <WonAtomContentGeneral atomUri={this.props.atomUri} />

            {this.props.hasContent && (
              <WonAtomContentDetails
                atomUri={this.props.atomUri}
                branch="content"
              />
            )}
            {this.props.hasContent &&
              this.props.hasSeeksBranch && (
                <WonLabelledHr label="Search" className="cp__labelledhr" />
              )}
            {this.props.hasSeeksBranch && (
              <WonAtomContentDetails
                atomUri={this.props.atomUri}
                branch="seeks"
              />
            )}
          </React.Fragment>
        );
      } else if (this.isSelectedTab("HELDBY")) {
        if (this.props.isHeld) {
          visibleTabFragment = (
            <WonAtomContentPersona holdsUri={this.props.atomUri} />
          );
        } else if (
          this.props.isActive &&
          this.props.hasHoldableSocket &&
          this.props.isOwned
        ) {
          visibleTabFragment = (
            <ElmReact
              src={Elm.AddPersona}
              flags={{
                post: this.props.atom.toJS(),
                personas: this.props.personas.toJS(),
              }}
            />
          );
        }
      } else if (this.isSelectedTab("PARTICIPANTS")) {
        visibleTabFragment = (
          <WonAtomContentParticipants atomUri={this.props.atomUri} />
        );
      } else if (this.isSelectedTab("BUDDIES")) {
        visibleTabFragment = (
          <WonAtomContentBuddies atomUri={this.props.atomUri} />
        );
      } else if (this.isSelectedTab("REVIEWS")) {
        visibleTabFragment = (
          <div className="atom-content__reviews">
            <div className="atom-content__reviews__empty">
              No Reviews to display.
            </div>
          </div>
        );
      } else if (this.isSelectedTab("SUGGESTIONS")) {
        visibleTabFragment = (
          <WonAtomContentSuggestions atomUri={this.props.atomUri} />
        );
      } else if (this.isSelectedTab("HOLDS")) {
        visibleTabFragment = (
          <WonAtomContentHolds atomUri={this.props.atomUri} />
        );
      } else if (this.isSelectedTab("RDF")) {
        visibleTabFragment = (
          <div className="atom-info__content__rdf">
            <a
              className="rdflink clickable"
              target="_blank"
              rel="noopener noreferrer"
              href={this.props.atomUri}
            >
              <svg className="rdflink__small">
                <use xlinkHref="#rdf_logo_1" href="#rdf_logo_1" />
              </svg>
              <span className="rdflink__label">Atom</span>
            </a>
            {this.props.openConnectionUri && (
              <a
                className="rdflink clickable"
                target="_blank"
                rel="noopener noreferrer"
                href={this.props.openConnectionUri}
              >
                <svg className="rdflink__small">
                  <use xlinkHref="#rdf_logo_1" href="#rdf_logo_1" />
                </svg>
                <span className="rdflink__label">Connection</span>
              </a>
            )}
            {this.props.atom.get("jsonld") && (
              <WonTrig jsonld={this.props.atom.get("jsonld")} />
            )}
          </div>
        );
      }

      return (
        <won-atom-content>
          <div className="atom-content">
            {processingUpdateElement}
            {visibleTabFragment}
          </div>
        </won-atom-content>
      );
    }
  }

  tryReload() {
    if (this.props.atomUri && this.props.atomFailedToLoad) {
      this.props.fetchAtom(this.props.atomUri);
    }
  }

  isSelectedTab(tabName) {
    return tabName === this.props.visibleTab;
  }
}
WonAtomContent.propTypes = {
  atomUri: PropTypes.string.isRequired,
  hasContent: PropTypes.bool,
  hasSeeksBranch: PropTypes.bool,
  atom: PropTypes.object,
  isOwned: PropTypes.bool,
  isActive: PropTypes.bool,
  isHeld: PropTypes.bool,
  hasChatSocket: PropTypes.bool,
  hasHoldableSocket: PropTypes.bool,
  atomLoading: PropTypes.bool,
  atomFailedToLoad: PropTypes.bool,
  atomProcessingUpdate: PropTypes.bool,
  createdTimestamp: PropTypes.any,
  shouldShowRdf: PropTypes.bool,
  fromConnection: PropTypes.bool,
  openConnectionUri: PropTypes.string,
  visibleTab: PropTypes.string,
  personas: PropTypes.object,
  fetchAtom: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(WonAtomContent);
