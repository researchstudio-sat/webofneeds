import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../../actions/actions.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import { get, getIn, sortByDate } from "../../utils.js";
import * as processSelectors from "../../redux/selectors/process-selectors.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as useCaseUtils from "../../usecase-utils.js";
import * as wonLabelUtils from "../../won-label-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";

import WonTopnav from "../../components/topnav.jsx";
import WonModalDialog from "../../components/modal-dialog.jsx";
import WonMenu from "../../components/menu.jsx";
import WonToasts from "../../components/toasts.jsx";
import WonSlideIn from "../../components/slide-in.jsx";
import WonAtomCardGrid from "../../components/atom-card-grid";
import WonFooter from "../../components/footer";

import "~/style/_overview.scss";
import ico16_arrow_down from "~/images/won-icons/ico16_arrow_down.svg";
import { withRouter } from "react-router-dom";

const mapStateToProps = (state, ownProps) => {
  const debugModeEnabled = viewSelectors.isDebugModeEnabled(state);

  const whatsNewAtoms = generalSelectors
    .getWhatsNewAtoms(state)
    .filter(metaAtom => atomUtils.isActive(metaAtom))
    .filter(metaAtom => debugModeEnabled || !atomUtils.isSearchAtom(metaAtom))
    .filter(
      metaAtom => debugModeEnabled || !atomUtils.isDirectResponseAtom(metaAtom)
    )
    .filter(
      metaAtom => debugModeEnabled || !atomUtils.isInvisibleAtom(metaAtom)
    )
    .filter(
      (metaAtom, metaAtomUri) =>
        !generalSelectors.isAtomOwned(state, metaAtomUri)
    );

  const whatsNewUseCaseIdentifierArray = whatsNewAtoms
    .map(atom => getIn(atom, ["matchedUseCase", "identifier"]))
    .filter(identifier => !!identifier)
    .toSet()
    .toArray();

  const sortedVisibleAtoms = sortByDate(whatsNewAtoms, "creationDate");
  const sortedVisibleAtomUriArray = sortedVisibleAtoms && [
    ...sortedVisibleAtoms.flatMap(visibleAtom => get(visibleAtom, "uri")),
  ];
  const lastAtomUrisUpdateDate = getIn(state, [
    "owner",
    "lastWhatsNewUpdateTime",
  ]);

  const isOwnerAtomUrisLoading = processSelectors.isProcessingWhatsNew(state);
  const isOwnerAtomUrisToLoad =
    !lastAtomUrisUpdateDate && !isOwnerAtomUrisLoading;

  const accountState = get(state, "account");

  return {
    whatsNewUseCaseIdentifierArray: whatsNewUseCaseIdentifierArray,
    whatsNewAtoms: whatsNewAtoms,
    isLoggedIn: accountUtils.isLoggedIn(accountState),
    currentLocation: generalSelectors.getCurrentLocation(state),
    lastAtomUrisUpdateDate,
    friendlyLastAtomUrisUpdateTimestamp:
      lastAtomUrisUpdateDate &&
      wonLabelUtils.relativeTime(
        generalSelectors.selectLastUpdateTime(state),
        lastAtomUrisUpdateDate
      ),
    sortedVisibleAtomUriArray,
    hasVisibleAtomUris:
      sortedVisibleAtomUriArray && sortedVisibleAtomUriArray.length > 0,
    sortedVisibleAtomUriSize: sortedVisibleAtomUriArray
      ? sortedVisibleAtomUriArray.length
      : 0,
    isOwnerAtomUrisLoading,
    isOwnerAtomUrisToLoad,
    showSlideIns:
      viewSelectors.hasSlideIns(state, ownProps.history) &&
      viewSelectors.isSlideInsVisible(state),
    showModalDialog: viewSelectors.showModalDialog(state),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    fetchWhatsNew: date => {
      dispatch(actionCreators.atoms__fetchWhatsNew(date));
    },
  };
};

class PageOverview extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      open: [],
    };
    this.reload = this.reload.bind(this);
  }

  render() {
    return (
      <section className={!this.props.isLoggedIn ? "won-signed-out" : ""}>
        {this.props.showModalDialog && <WonModalDialog />}
        <WonTopnav pageTitle="What's New" />
        {this.props.isLoggedIn && <WonMenu />}
        <WonToasts />
        {this.props.showSlideIns && <WonSlideIn />}
        <main className="owneroverview">
          <div className="owneroverview__header">
            <div className="owneroverview__header__title">
              {"What's new? "}
              {!this.props.isOwnerAtomUrisLoading &&
                this.props.hasVisibleAtomUris &&
                !this.props.whatsNewUseCaseIdentifierArray && (
                  <span className="owneroverview__header__title__count">
                    {"(" + this.props.sortedVisibleAtomUriSize + ")"}
                  </span>
                )}
            </div>
            <div className="owneroverview__header__updated">
              {this.props.isOwnerAtomUrisLoading ? (
                <div className="owneroverview__header__updated__loading hide-in-responsive">
                  Loading...
                </div>
              ) : (
                <div className="owneroverview__header__updated__time hide-in-responsive">
                  {"Updated: " + this.props.friendlyLastAtomUrisUpdateTimestamp}
                </div>
              )}
              <button
                className="owneroverview__header__updated__reload won-button--filled red"
                onClick={this.reload}
                disabled={this.props.isOwnerAtomUrisLoading}
              >
                Reload
              </button>
            </div>
          </div>
          {this.props.hasVisibleAtomUris ? (
            <div className="owneroverview__usecases">
              {this.props.whatsNewUseCaseIdentifierArray.map(
                (ucIdentifier, index) => (
                  <div
                    className="owneroverview__usecases__usecase"
                    key={ucIdentifier + "-" + index}
                  >
                    <div
                      className="owneroverview__usecases__usecase__header clickable"
                      onClick={() => this.toggleUseCase(ucIdentifier)}
                    >
                      {useCaseUtils.getUseCaseIcon(ucIdentifier) && (
                        <svg className="owneroverview__usecases__usecase__header__icon">
                          <use
                            xlinkHref={useCaseUtils.getUseCaseIcon(
                              ucIdentifier
                            )}
                            href={useCaseUtils.getUseCaseIcon(ucIdentifier)}
                          />
                        </svg>
                      )}
                      <div className="owneroverview__usecases__usecase__header__title">
                        {useCaseUtils.getUseCaseLabel(ucIdentifier)}
                        <span className="owneroverview__usecases__usecase__header__title__count">
                          {"(" + this.getAtomsSizeByUseCase(ucIdentifier) + ")"}
                        </span>
                      </div>
                      <svg
                        className={
                          "owneroverview__usecases__usecase__header__carret " +
                          (this.isUseCaseExpanded(ucIdentifier)
                            ? " owneroverview__usecases__usecase__header__carret--expanded "
                            : " owneroverview__usecases__usecase__header__carret--collapsed ")
                        }
                      >
                        <use
                          xlinkHref={ico16_arrow_down}
                          href={ico16_arrow_down}
                        />
                      </svg>
                    </div>
                    {this.isUseCaseExpanded(ucIdentifier) && (
                      <div className="owneroverview__usecases__usecase__atoms">
                        <WonAtomCardGrid
                          atomUris={this.getSortedVisibleAtomUriArrayByUseCase(
                            ucIdentifier
                          )}
                          currentLocation={this.props.currentLocation}
                          showSuggestions={false}
                          showHolder={true}
                          showCreate={false}
                        />
                      </div>
                    )}
                  </div>
                )
              )}
              {this.hasOtherAtoms() && (
                <div className="owneroverview__usecases__usecase">
                  {this.props.whatsNewUseCaseIdentifierArray && (
                    <div
                      className="owneroverview__usecases__usecase__header"
                      onClick={() => this.toggleUseCase(undefined)}
                    >
                      <div className="owneroverview__usecases__usecase__header__title">
                        Other
                        <span className="owneroverview__usecases__usecase__header__title__count">
                          {"(" + this.getOtherAtomsSize() + ")"}
                        </span>
                      </div>
                      <svg
                        className={
                          "owneroverview__usecases__usecase__header__carret " +
                          (this.isUseCaseExpanded(undefined)
                            ? " owneroverview__usecases__usecase__header__carret--expanded "
                            : " owneroverview__usecases__usecase__header__carret--collapsed ")
                        }
                      >
                        <use
                          xlinkHref={ico16_arrow_down}
                          href={ico16_arrow_down}
                        />
                      </svg>
                    </div>
                  )}

                  {this.isUseCaseExpanded(undefined) && (
                    <div className="owneroverview__usecases__usecase__atoms">
                      <WonAtomCardGrid
                        atomUris={this.getSortedVisibleOtherAtomUriArray()}
                        currentLocation={this.props.currentLocation}
                        showSuggestions={false}
                        showHolder={true}
                        showCreate={false}
                      />
                    </div>
                  )}
                </div>
              )}
            </div>
          ) : (
            <div className="owneroverview__noresults">
              <span className="owneroverview__noresults__label">
                Nothing new found.
              </span>
            </div>
          )}
        </main>
        <WonFooter />
      </section>
    );
  }

  hasOtherAtoms() {
    return !!this.props.whatsNewAtoms.find(
      atom => !atomUtils.hasMatchedUseCase(atom)
    );
  }
  getOtherAtomsSize() {
    const useCaseAtoms = this.props.whatsNewAtoms.filter(
      atom => !atomUtils.hasMatchedUseCase(atom)
    );
    return useCaseAtoms ? useCaseAtoms.size : 0;
  }

  getAtomsSizeByUseCase(ucIdentifier) {
    const useCaseAtoms = this.props.whatsNewAtoms.filter(
      atom => atomUtils.getMatchedUseCaseIdentifier(atom) === ucIdentifier
    );
    return useCaseAtoms ? useCaseAtoms.size : 0;
  }

  getSortedVisibleAtomUriArrayByUseCase(ucIdentifier) {
    const useCaseAtoms = this.props.whatsNewAtoms.filter(
      atom => atomUtils.getMatchedUseCaseIdentifier(atom) === ucIdentifier
    );
    const sortedUseCaseAtoms = sortByDate(useCaseAtoms, "creationDate");
    return (
      sortedUseCaseAtoms && [
        ...sortedUseCaseAtoms.flatMap(atom => get(atom, "uri")),
      ]
    );
  }

  getSortedVisibleOtherAtomUriArray() {
    const useCaseAtoms = this.props.whatsNewAtoms.filter(
      atom => !atomUtils.hasMatchedUseCase(atom)
    );
    const sortedUseCaseAtoms = sortByDate(useCaseAtoms, "creationDate");
    return (
      sortedUseCaseAtoms && [
        ...sortedUseCaseAtoms.flatMap(atom => get(atom, "uri")),
      ]
    );
  }

  isUseCaseExpanded(ucIdentifier) {
    return this.state.open.includes(ucIdentifier);
  }

  toggleUseCase(ucIdentifier) {
    if (this.isUseCaseExpanded(ucIdentifier)) {
      this.setState({
        open: this.state.open.filter(element => ucIdentifier !== element),
      });
    } else {
      const _open = this.state.open;
      _open.push(ucIdentifier);
      this.setState({ open: _open });
    }
  }

  componentDidUpdate() {
    if (this.props.isOwnerAtomUrisToLoad) {
      this.props.fetchWhatsNew();
    }
  }

  reload() {
    if (!this.props.isOwnerAtomUrisLoading) {
      const modifiedAfterDate =
        (this.props.lastAtomUrisUpdateDate &&
          new Date(this.props.lastAtomUrisUpdateDate)) ||
        new Date(Date.now() - 30 /*Days before*/ * 86400000);
      this.props.fetchWhatsNew(modifiedAfterDate);
    }
  }
}
PageOverview.propTypes = {
  whatsNewUseCaseIdentifierArray: PropTypes.arrayOf(PropTypes.string),
  whatsNewAtoms: PropTypes.object,
  isLoggedIn: PropTypes.bool,
  currentLocation: PropTypes.object,
  lastAtomUrisUpdateDate: PropTypes.number,
  friendlyLastAtomUrisUpdateTimestamp: PropTypes.string,
  sortedVisibleAtomUriArray: PropTypes.arrayOf(PropTypes.string),
  hasVisibleAtomUris: PropTypes.bool,
  sortedVisibleAtomUriSize: PropTypes.number,
  isOwnerAtomUrisLoading: PropTypes.bool,
  isOwnerAtomUrisToLoad: PropTypes.bool,
  showSlideIns: PropTypes.bool,
  showModalDialog: PropTypes.bool,
  fetchWhatsNew: PropTypes.func,
};

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(PageOverview)
);
