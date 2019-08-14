import React from "react";
import PropTypes from "prop-types";
import { actionCreators } from "../actions/actions.js";
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

export default class WonAtomContent extends React.Component {
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
    const openConnectionUri = generalSelectors.getConnectionUriFromRoute(state);
    const atom = getIn(state, ["atoms", this.atomUri]);
    const isOwned = generalSelectors.isAtomOwned(state, this.atomUri);
    const isActive = atomUtils.isActive(atom);
    const content = get(atom, "content");

    //TODO it will be possible to have more than one seeks
    const seeks = get(atom, "seeks");

    const hasContent = this.hasVisibleDetails(content);
    const hasSeeksBranch = this.hasVisibleDetails(seeks);

    const viewState = get(state, "view");
    const process = get(state, "process");

    return {
      hasContent,
      hasSeeksBranch,
      atom,
      isOwned,
      isActive,
      isHeld: atomUtils.isHeld(atom),
      hasChatSocket: atomUtils.hasChatSocket(atom),
      hasHoldableSocket: atomUtils.hasHoldableSocket(atom),
      atomLoading: !atom || processUtils.isAtomLoading(process, this.atomUri),
      atomFailedToLoad:
        atom && processUtils.hasAtomFailedToLoad(process, this.atomUri),
      atomProcessingUpdate:
        atom && processUtils.isAtomProcessingUpdate(process, this.atomUri),
      createdTimestamp: atom && atom.get("creationDate"),
      shouldShowRdf: viewUtils.showRdf(viewState),
      fromConnection: !!openConnectionUri,
      openConnectionUri,
      visibleTab: viewUtils.getVisibleTabByAtomUri(viewState, this.atomUri),
      personas: generalSelectors.getOwnedCondensedPersonaList(state),
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    if (this.state.atomLoading) {
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
    } else if (this.state.atomFailedToLoad) {
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
      const processingUpdateElement = this.state.atomProcessingUpdate && (
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
            <WonAtomContentGeneral
              atomUri={this.atomUri}
              ngRedux={this.props.ngRedux}
            />

            {this.state.hasContent && (
              <WonAtomContentDetails
                atomUri={this.atomUri}
                branch="content"
                ngRedux={this.props.ngRedux}
              />
            )}
            {this.state.hasContent &&
              this.state.hasSeeksBranch && (
                <WonLabelledHr
                  label="Search"
                  className="cp__labelledhr"
                  ngRedux={this.props.ngRedux}
                />
              )}
            {this.state.hasSeeksBranch && (
              <WonAtomContentDetails
                atomUri={this.atomUri}
                branch="seeks"
                ngRedux={this.props.ngRedux}
              />
            )}
          </React.Fragment>
        );
      } else if (this.isSelectedTab("HELDBY")) {
        if (this.state.isHeld) {
          visibleTabFragment = (
            <WonAtomContentPersona
              holdsUri={this.atomUri}
              ngRedux={this.props.ngRedux}
            />
          );
        } else if (
          this.state.isActive &&
          this.state.hasHoldableSocket &&
          this.state.isOwned
        ) {
          visibleTabFragment = (
            <ElmReact
              src={Elm.AddPersona}
              ngRedux={this.props.ngRedux}
              flags={{
                post: this.state.atom.toJS(),
                personas: this.state.personas.toJS(),
              }}
            />
          );
        }
      } else if (this.isSelectedTab("PARTICIPANTS")) {
        visibleTabFragment = (
          <WonAtomContentParticipants
            atomUri={this.atomUri}
            ngRedux={this.props.ngRedux}
          />
        );
      } else if (this.isSelectedTab("BUDDIES")) {
        visibleTabFragment = (
          <WonAtomContentBuddies
            atomUri={this.atomUri}
            ngRedux={this.props.ngRedux}
          />
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
          <WonAtomContentSuggestions
            atomUri={this.atomUri}
            ngRedux={this.props.ngRedux}
          />
        );
      } else if (this.isSelectedTab("HOLDS")) {
        visibleTabFragment = (
          <WonAtomContentHolds
            atomUri={this.atomUri}
            ngRedux={this.props.ngRedux}
          />
        );
      } else if (this.isSelectedTab("RDF")) {
        visibleTabFragment = (
          <div className="atom-info__content__rdf">
            <a
              className="rdflink clickable"
              target="_blank"
              rel="noopener noreferrer"
              href={this.atomUri}
            >
              <svg className="rdflink__small">
                <use xlinkHref="#rdf_logo_1" href="#rdf_logo_1" />
              </svg>
              <span className="rdflink__label">Atom</span>
            </a>
            {this.state.openConnectionUri && (
              <a
                className="rdflink clickable"
                target="_blank"
                rel="noopener noreferrer"
                href={this.state.openConnectionUri}
              >
                <svg className="rdflink__small">
                  <use xlinkHref="#rdf_logo_1" href="#rdf_logo_1" />
                </svg>
                <span className="rdflink__label">Connection</span>
              </a>
            )}
            {this.state.atom.get("jsonld") && (
              <WonTrig jsonld={this.state.atom.get("jsonld")} />
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
    if (this.atomUri && this.state.atomFailedToLoad) {
      this.props.ngRedux.dispatch(
        actionCreators.atoms__fetchUnloadedAtom(this.atomUri)
      );
    }
  }

  isSelectedTab(tabName) {
    return tabName === this.state.visibleTab;
  }

  /**
   * This function checks if there is at least one detail present that is displayable
   */
  hasVisibleDetails(contentBranchImm) {
    return (
      contentBranchImm &&
      contentBranchImm.find(
        (detailValue, detailKey) =>
          detailKey != "type" &&
          detailKey != "sockets" &&
          detailKey != "defaultSocket"
      )
    );
  }
}
WonAtomContent.propTypes = {
  atomUri: PropTypes.string.isRequired,
  ngRedux: PropTypes.object.isRequired,
};
