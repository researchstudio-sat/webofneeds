import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../../actions/actions.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import { get, getIn, sortByDate } from "../../utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import * as processUtils from "../../redux/utils/process-utils.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import * as viewUtils from "../../redux/utils/view-utils.js";
import WonModalDialog from "../../components/modal-dialog.jsx";
import WonAtomMessages from "../../components/atom-messages.jsx";
import WonAtomInfo from "../../components/atom-info.jsx";
import WonTopnav from "../../components/topnav.jsx";
import WonMenu from "../../components/menu.jsx";
import WonToasts from "../../components/toasts.jsx";
import WonSlideIn from "../../components/slide-in.jsx";
import WonFooter from "../../components/footer.jsx";
import WonHowTo from "../../components/howto.jsx";
import WonAtomCardGrid from "../../components/atom-card-grid.jsx";

import "~/style/_inventory.scss";
import "~/style/_connection-overlay.scss";

const mapStateToProps = state => {
  const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);

  const ownedActivePersonas = generalSelectors
    .getOwnedPersonas(state)
    .filter(atom => atomUtils.isActive(atom));
  const ownedUnassignedActivePosts = generalSelectors
    .getOwnedPosts(state)
    .filter(atom => atomUtils.isActive(atom))
    .filter(atom => !atomUtils.isHeld(atom));
  const ownedInactiveAtoms = generalSelectors
    .getOwnedAtoms(state)
    .filter(atom => atomUtils.isInactive(atom));

  const sortedOwnedUnassignedActivePosts = sortByDate(
    ownedUnassignedActivePosts,
    "creationDate"
  );
  const sortedOwnedUnassignedAtomUriArray = sortedOwnedUnassignedActivePosts
    ? [...sortedOwnedUnassignedActivePosts.flatMap(atom => get(atom, "uri"))]
    : [];

  const sortedOwnedInactiveAtoms = sortByDate(
    ownedInactiveAtoms,
    "creationDate"
  );
  const sortedOwnedInactiveAtomUriArray = sortedOwnedInactiveAtoms
    ? [...sortedOwnedInactiveAtoms.flatMap(atom => get(atom, "uri"))]
    : [];

  const sortedOwnedActivePersonas = sortByDate(
    ownedActivePersonas,
    "modifiedDate"
  );

  const sortedOwnedActivePersonaUriArray = sortedOwnedActivePersonas
    ? [...sortedOwnedActivePersonas.flatMap(atom => get(atom, "uri"))]
    : [];

  const viewState = get(state, "view");

  const theme = getIn(state, ["config", "theme"]);
  const themeName = get(theme, "name");
  const welcomeTemplate = get(theme, "welcomeTemplate");

  const accountState = get(state, "account");
  const process = get(state, "process");

  return {
    isInitialLoadInProgress: processUtils.isProcessingInitialLoad(process),
    isLoggedIn: accountUtils.isLoggedIn(accountState),
    welcomeTemplatePath: "./skin/" + themeName + "/" + welcomeTemplate,
    currentLocation: generalSelectors.getCurrentLocation(state),
    sortedOwnedUnassignedAtomUriArray,
    sortedOwnedInactiveAtomUriArray,
    hasOwnedUnassignedAtomUris:
      sortedOwnedUnassignedAtomUriArray &&
      sortedOwnedUnassignedAtomUriArray.length > 0,
    hasOwnedInactiveAtomUris:
      sortedOwnedInactiveAtomUriArray &&
      sortedOwnedInactiveAtomUriArray.length > 0,
    unassignedAtomSize: sortedOwnedUnassignedAtomUriArray
      ? sortedOwnedUnassignedAtomUriArray.length
      : 0,
    inactiveAtomUriSize: sortedOwnedInactiveAtomUriArray
      ? sortedOwnedInactiveAtomUriArray.length
      : 0,
    hasOwnedActivePersonas: sortedOwnedActivePersonaUriArray.length > 0,
    sortedOwnedActivePersonaUriArray,
    showSlideIns:
      viewSelectors.hasSlideIns(state) &&
      viewSelectors.isSlideInsVisible(state),
    showModalDialog: viewSelectors.showModalDialog(state),
    showConnectionOverlay: !!viewConnUri,
    viewConnUri,
    showClosedAtoms: viewUtils.showClosedAtoms(viewState),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    toggleClosedAtoms: () => {
      dispatch(actionCreators.view__toggleClosedAtoms());
    },
  };
};

class PageInventory extends React.Component {
  render() {
    return (
      <section className={!this.props.isLoggedIn ? "won-signed-out" : ""}>
        {this.props.showModalDialog && <WonModalDialog />}
        {this.props.showConnectionOverlay && (
          <div className="won-modal-connectionview">
            <WonAtomMessages connectionUri={this.props.viewConnUri} />
          </div>
        )}
        <WonTopnav pageTitle="Inventory" />
        {this.props.isLoggedIn && <WonMenu />}
        <WonToasts />
        {this.props.showSlideIns && <WonSlideIn />}

        {this.props.isLoggedIn ? (
          this.props.isInitialLoadInProgress ? (
            <main className="ownerloading">
              <svg className="ownerloading__spinner hspinner">
                <use xlinkHref="#ico_loading_anim" href="#ico_loading_anim" />
              </svg>
              <span className="ownerloading__label">
                Gathering your Atoms...
              </span>
            </main>
          ) : (
            <main className="ownerinventory">
              {this.props.hasOwnedActivePersonas && (
                <div className="ownerinventory__personas">
                  {this.props.sortedOwnedActivePersonaUriArray.map(
                    (personaUri, index) => (
                      <WonAtomInfo
                        key={personaUri + "-" + index}
                        className="ownerinventory__personas__persona"
                        atomUri={personaUri}
                      />
                    )
                  )}
                </div>
              )}
              <div className="ownerinventory__header">
                <div className="ownerinventory__header__title">
                  Unassigned
                  {this.props.hasOwnedUnassignedAtomUris && (
                    <span className="ownerinventory__header__title__count">
                      {"(" + this.props.unassignedAtomSize + ")"}
                    </span>
                  )}
                </div>
              </div>
              <div className="ownerinventory__content">
                <WonAtomCardGrid
                  atomUris={this.props.sortedOwnedUnassignedAtomUriArray}
                  currentLocation={this.props.currentLocation}
                  showSuggestions={true}
                  showPersona={true}
                  showCreate={true}
                />
              </div>
              {this.props.hasOwnedInactiveAtomUris && (
                <div className="ownerinventory__header">
                  <div className="ownerinventory__header__title">
                    Archived
                    <span className="ownerinventory__header__title__count">
                      {"(" + this.props.inactiveAtomUriSize + ")"}
                    </span>
                  </div>
                  <svg
                    className={
                      "ownerinventory__header__carret " +
                      (this.props.showClosedAtoms
                        ? "ownerinventory__header__carret--expanded"
                        : "ownerinventory__header__carret--collapsed")
                    }
                    onClick={this.props.toggleClosedAtoms}
                  >
                    <use
                      xlinkHref="#ico16_arrow_down"
                      href="#ico16_arrow_down"
                    />
                  </svg>
                </div>
              )}
              {this.props.showClosedAtoms &&
                this.props.hasOwnedInactiveAtomUris && (
                  <div className="ownerinventory__content">
                    <WonAtomCardGrid
                      atomUris={this.props.sortedOwnedInactiveAtomUriArray}
                      currentLocation={this.props.currentLocation}
                      showSuggestions={false}
                      showPersona={false}
                      showCreate={false}
                    />
                  </div>
                )}
            </main>
          )
        ) : (
          <main className="ownerwelcome">
            <div
              className="ownerwelcome__text"
              dangerouslySetInnerHTML={{
                __html: this.props.welcomeTemplatePath,
              }}
            />
            <WonHowTo />
          </main>
        )}

        <WonFooter />
      </section>
    );
  }
}
PageInventory.propTypes = {
  isLoggedIn: PropTypes.bool,
  isInitialLoadInProgress: PropTypes.bool,
  welcomeTemplatePath: PropTypes.string,
  currentLocation: PropTypes.object,
  sortedOwnedUnassignedAtomUriArray: PropTypes.arrayOf(PropTypes.string),
  sortedOwnedInactiveAtomUriArray: PropTypes.arrayOf(PropTypes.string),
  hasOwnedUnassignedAtomUris: PropTypes.bool,
  hasOwnedInactiveAtomUris: PropTypes.bool,
  unassignedAtomSize: PropTypes.number,
  inactiveAtomUriSize: PropTypes.number,
  hasOwnedActivePersonas: PropTypes.bool,
  sortedOwnedActivePersonaUriArray: PropTypes.arrayOf(PropTypes.string),
  showSlideIns: PropTypes.bool,
  showModalDialog: PropTypes.bool,
  showConnectionOverlay: PropTypes.bool,
  viewConnUri: PropTypes.string,
  showClosedAtoms: PropTypes.bool,
  toggleClosedAtoms: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(PageInventory);
