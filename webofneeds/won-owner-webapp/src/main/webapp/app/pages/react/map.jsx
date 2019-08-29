import React from "react";
import PropTypes from "prop-types";
import Immutable from "immutable";
import { connect } from "react-redux";
import { actionCreators } from "../../actions/actions.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import { get, getIn } from "../../utils.js";
import * as processUtils from "../../redux/utils/process-utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as wonLabelUtils from "../../won-label-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import {
  reverseSearchNominatim,
  scrubSearchResults,
  searchNominatim,
} from "../../api/nominatim-api.js";

import WonTopnav from "../../components/topnav.jsx";
import WonModalDialog from "../../components/modal-dialog.jsx";
import WonAtomMessages from "../../components/atom-messages.jsx";
import WonAtomMap from "../../components/atom-map.jsx";
import WonMenu from "../../components/menu.jsx";
import WonToasts from "../../components/toasts.jsx";
import WonSlideIn from "../../components/slide-in.jsx";
import WonAtomCardGrid from "../../components/atom-card-grid.jsx";
import WonFooter from "../../components/footer.jsx";
import WonTitlePicker from "../../components/details/picker/title-picker.jsx";

import "~/style/_map.scss";
import "~/style/_connection-overlay.scss";

const mapStateToProps = state => {
  const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);

  const isLocationAccessDenied = generalSelectors.isLocationAccessDenied(state);
  const currentLocation = generalSelectors.getCurrentLocation(state);

  const lastWhatsAroundLocation = getIn(state, [
    "owner",
    "lastWhatsAroundLocation",
  ]);
  const whatsAroundMaxDistance = getIn(state, [
    "owner",
    "lastWhatsAroundMaxDistance",
  ]);

  const whatsAroundMetaAtoms = generalSelectors
    .getWhatsAroundAtoms(state)
    .filter(metaAtom => atomUtils.isActive(metaAtom))
    .filter(metaAtom => !atomUtils.isSearchAtom(metaAtom))
    .filter(metaAtom => !atomUtils.isDirectResponseAtom(metaAtom))
    .filter(metaAtom => !atomUtils.isInvisibleAtom(metaAtom))
    .filter(
      (metaAtom, metaAtomUri) =>
        !generalSelectors.isAtomOwned(state, metaAtomUri)
    )
    .filter(metaAtom => atomUtils.hasLocation(metaAtom))
    .filter(metaAtom => {
      const distanceFrom = atomUtils.getDistanceFrom(
        metaAtom,
        lastWhatsAroundLocation
      );
      if (distanceFrom) {
        return distanceFrom <= whatsAroundMaxDistance;
      }
      return false;
    });

  const sortedVisibleAtoms = atomUtils.sortByDistanceFrom(
    whatsAroundMetaAtoms,
    lastWhatsAroundLocation
  );
  const sortedVisibleAtomUriArray = sortedVisibleAtoms && [
    ...sortedVisibleAtoms.flatMap(visibleAtom => get(visibleAtom, "uri")),
  ];

  const lastAtomUrisUpdateDate = getIn(state, [
    "owner",
    "lastWhatsAroundUpdateTime",
  ]);

  const process = get(state, "process");
  const isOwnerAtomUrisLoading = processUtils.isProcessingWhatsAround(process);
  const isOwnerAtomUrisToLoad =
    !lastAtomUrisUpdateDate && !isOwnerAtomUrisLoading;

  let locations = [];
  whatsAroundMetaAtoms &&
    whatsAroundMetaAtoms.map(atom => {
      const atomLocation = atomUtils.getLocation(atom);
      locations.push(atomLocation);
    });

  const accountState = get(state, "account");

  return {
    isLoggedIn: accountUtils.isLoggedIn(accountState),
    currentLocation,
    isLocationAccessDenied,
    lastWhatsAroundLocation,
    locations,
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
      viewSelectors.hasSlideIns(state) &&
      viewSelectors.isSlideInsVisible(state),
    showModalDialog: viewSelectors.showModalDialog(state),
    showConnectionOverlay: !!viewConnUri,
    viewConnUri,
  };
};

const mapDispatchToProps = dispatch => {
  return {
    fetchWhatsAround: (date, latlng, distance) => {
      dispatch(actionCreators.atoms__fetchWhatsAround(date, latlng, distance));
    },
    locationAccessDenied: () => {
      dispatch(actionCreators.view__locationAccessDenied());
    },
    updateCurrentLocation: locImm => {
      dispatch(actionCreators.view__updateCurrentLocation(locImm));
    },
  };
};

class PageMap extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      showLocationInput: false,
      searchText: "",
      searchResults: [],
      lastWhatsAroundLocationName: "",
    };

    this.reload = this.reload.bind(this);
    this.selectCurrentLocation = this.selectCurrentLocation.bind(this);
    this.updateWhatsAroundSuggestions = this.updateWhatsAroundSuggestions.bind(
      this
    );
    this.fetchCurrentLocationAndReload = this.fetchCurrentLocationAndReload.bind(
      this
    );
  }

  render() {
    return (
      <section className={!this.props.isLoggedIn ? "won-signed-out" : ""}>
        {this.props.showModalDialog && <WonModalDialog />}
        {this.props.showConnectionOverlay && (
          <div className="won-modal-connectionview">
            <WonAtomMessages connectionUri={this.props.viewConnUri} />
          </div>
        )}
        <WonTopnav pageTitle="What's around" />
        {this.props.isLoggedIn && <WonMenu />}
        <WonToasts />
        {this.props.showSlideIns && <WonSlideIn />}
        <main className="ownermap">
          {(this.props.isLocationAccessDenied ||
            this.props.lastWhatsAroundLocation) && (
            <div className="ownermap__header">
              <span className="ownermap__header__label">
                {"What's around:"}
              </span>
              {!this.state.showLocationInput && (
                <div
                  className="ownermap__header__location"
                  onClick={() => this.setState({ showLocationInput: true })}
                >
                  <svg className="ownermap__header__location__icon">
                    <use
                      xlinkHref="#ico36_detail_location"
                      href="#ico36_detail_location"
                    />
                  </svg>
                  <span className="ownermap__header__location__label">
                    {this.state.lastWhatsAroundLocationName}
                  </span>
                </div>
              )}
              {(this.state.showLocationInput ||
                (this.props.isLocationAccessDenied &&
                  !this.props.lastWhatsAroundLocation)) && (
                <div className="ownermap__header__input">
                  <svg
                    className="ownermap__header__input__icon"
                    onClick={() => this.setState({ showLocationInput: false })}
                  >
                    <use
                      xlinkHref="#ico36_detail_location"
                      href="#ico36_detail_location"
                    />
                  </svg>
                  <WonTitlePicker
                    onUpdate={this.updateWhatsAroundSuggestions}
                    initialValue={this.state.searchText}
                    detail={{ placeholder: "Search around location" }}
                  />
                </div>
              )}

              <div className="ownermap__header__updated">
                {!this.state.showLocationInput &&
                  (this.props.isOwnerAtomUrisLoading ? (
                    <div className="ownermap__header__updated__loading hide-in-responsive">
                      Loading...
                    </div>
                  ) : (
                    <div className="ownermap__header__updated__time hide-in-responsive">
                      {"Updated: " +
                        this.props.friendlyLastAtomUrisUpdateTimestamp}
                    </div>
                  ))}

                {this.state.showLocationInput ? (
                  <div
                    className="ownermap__header__updated__cancel won-button--filled red"
                    onClick={() => this.setState({ showLocationInput: false })}
                    disabled={this.props.isOwnerAtomUrisLoading}
                  >
                    Cancel
                  </div>
                ) : (
                  <div
                    className="ownermap__header__updated__reload won-button--filled red"
                    onClick={this.reload}
                    disabled={this.props.isOwnerAtomUrisLoading}
                  >
                    Reload
                  </div>
                )}
              </div>
            </div>
          )}
          {(!this.props.isOwnerAtomUrisToLoad ||
            this.props.isLocationAccessDenied) && (
            <div
              className={
                "ownermap__searchresults " +
                (this.state.showLocationInput ||
                (this.props.isLocationAccessDenied &&
                  !this.props.lastWhatsAroundLocation)
                  ? "ownermap__searchresults--visible"
                  : "")
              }
            >
              {!this.props.isLocationAccessDenied && (
                <div
                  className="ownermap__searchresults__result"
                  onClick={this.selectCurrentLocation}
                >
                  <svg className="ownermap__searchresults__result__icon">
                    <use
                      xlinkHref="#ico36_location_current"
                      href="#ico36_location_current"
                    />
                  </svg>
                  <div className="ownermap__searchresults__result__label">
                    Current Location
                  </div>
                </div>
              )}
              {this.state.searchResults.map((result, index) => (
                <div
                  className="ownermap__searchresults__result"
                  onClick={() => this.selectLocation(result)}
                  key={result.name + "-" + index}
                >
                  <svg className="ownermap__searchresults__result__icon">
                    <use
                      xlinkHref="#ico16_indicator_location"
                      href="#ico16_indicator_location"
                    />
                  </svg>
                  <div className="ownermap__searchresults__result__label">
                    {result.name}
                  </div>
                </div>
              ))}

              {this.props.isLocationAccessDenied &&
                !this.props.lastWhatsAroundLocation &&
                !this.props.hasVisibleAtomUris &&
                !(this.state.searchResults.length > 0) && (
                  <div className="ownermap__searchresults__deniedlocation">
                    <svg className="ownermap__searchresults__deniedlocation__icon">
                      <use
                        xlinkHref="#ico16_indicator_error"
                        href="#ico16_indicator_error"
                      />
                    </svg>
                    <div className="ownermap__searchresults__deniedlocation__label">
                      {
                        "You prohibit us from retrieving your location, so we won't be able to show what's around you. If you want to change that, grant access to the location in your browser and reload the page, or type any location in the input-field above."
                      }
                    </div>
                  </div>
                )}
            </div>
          )}
          {!this.props.currentLocation &&
            !this.props.isLocationAccessDenied &&
            !this.props.lastWhatsAroundLocation && (
              <div className="ownermap__nolocation">
                <svg className="ownermap__nolocation__icon">
                  <use
                    xlinkHref="#ico36_detail_location"
                    href="#ico36_detail_location"
                  />
                </svg>
                <div className="ownermap__nolocation__label">
                  {"You did not grant location access yet. "}
                  <span className="show-in-responsive">Tap</span>
                  <span className="hide-in-responsive">Click</span>
                  {
                    " the button below and accept the location access to see what is going on around you."
                  }
                </div>
                <div
                  className="ownermap__nolocation__button won-button--filled red"
                  onClick={this.fetchCurrentLocationAndReload}
                >
                  {"See What's Around"}
                </div>
              </div>
            )}

          {!this.props.isOwnerAtomUrisToLoad &&
          !!this.props.lastWhatsAroundLocation ? (
            <WonAtomMap
              className={
                "ownermap__map hide-in-responsive won-atom-map " +
                (!(
                  this.state.showLocationInput ||
                  (this.props.isLocationAccessDenied &&
                    !this.props.lastWhatsAroundLocation)
                )
                  ? " ownermap__map--visible "
                  : "")
              }
              locations={this.props.locations}
              currentLocation={this.props.lastWhatsAroundLocation}
            />
          ) : (
            undefined
          )}
          {this.props.lastWhatsAroundLocation &&
            (this.props.hasVisibleAtomUris ? (
              <div className="ownermap__content">
                <WonAtomCardGrid
                  atomUris={this.props.sortedVisibleAtomUriArray}
                  currentLocation={this.props.lastWhatsAroundLocation}
                  showPersona={true}
                  showSuggestions={false}
                  showCreate={false}
                />
              </div>
            ) : (
              <div className="ownermap__noresults">
                <span className="ownermap__noresults__label">
                  Nothing around this location, you can try another location by
                  clicking on the location in the header.
                </span>
              </div>
            ))}
        </main>
        <WonFooter />
      </section>
    );
  }

  componentDidUpdate(prevProps) {
    if (
      this.props.lastWhatsAroundLocation &&
      this.props.lastWhatsAroundLocation !== prevProps.lastWhatsAroundLocation
    ) {
      reverseSearchNominatim(
        this.props.lastWhatsAroundLocation.get("lat"),
        this.props.lastWhatsAroundLocation.get("lng"),
        13
      ).then(searchResult => {
        const displayName = searchResult.display_name;

        this.setState({
          searchText: displayName,
          lastWhatsAroundLocationName: displayName,
        });
      });
    }

    if (this.props.isOwnerAtomUrisToLoad && this.props.currentLocation) {
      const latlng = {
        lat: this.props.currentLocation.get("lat"),
        lng: this.props.currentLocation.get("lng"),
      };
      this.props.fetchWhatsAround(undefined, latlng, 5000);
    }
  }

  updateWhatsAroundSuggestions({ value }) {
    const whatsAroundInputValue = value && value.trim();
    if (!!whatsAroundInputValue && whatsAroundInputValue.length > 0) {
      searchNominatim(whatsAroundInputValue).then(searchResults => {
        const parsedResults = scrubSearchResults(
          searchResults,
          whatsAroundInputValue
        );

        this.setState({
          searchText: value,
          searchResults: parsedResults,
        });
      });
    } else {
      this.resetWhatsAroundInput();
    }
  }

  resetWhatsAroundInput() {
    this.setState({
      searchText: "",
      searchResults: [],
    });
  }

  reload() {
    if (
      !this.props.isOwnerAtomUrisLoading &&
      this.props.lastWhatsAroundLocation
    ) {
      const latlng = {
        lat: this.props.lastWhatsAroundLocation.get("lat"),
        lng: this.props.lastWhatsAroundLocation.get("lng"),
      };
      if (this.props.lastAtomUrisUpdateDate) {
        this.props.fetchWhatsAround(
          new Date(this.props.lastAtomUrisUpdateDate),
          latlng,
          5000
        );
      } else {
        this.props.fetchWhatsAround(undefined, latlng, 5000);
      }
    }
  }

  selectCurrentLocation() {
    if (!this.props.isLocationAccessDenied) {
      this.setState(
        {
          showLocationInput: false,
        },
        () => {
          if ("geolocation" in navigator) {
            navigator.geolocation.getCurrentPosition(
              currentLocation => {
                const lat = currentLocation.coords.latitude;
                const lng = currentLocation.coords.longitude;
                this.props.updateCurrentLocation(
                  Immutable.fromJS({ location: { lat, lng } })
                );

                if (this.props.lastAtomUrisUpdateDate) {
                  this.props.fetchWhatsAround(
                    new Date(this.props.lastAtomUrisUpdateDate),
                    { lat, lng },
                    5000
                  );
                } else {
                  this.props.fetchWhatsAround(undefined, { lat, lng }, 5000);
                }
              },
              error => {
                //error handler
                console.error(
                  "Could not retrieve geolocation due to error: ",
                  error.code,
                  ", continuing map initialization without currentLocation. fullerror:",
                  error
                );
                if (error.code == 1) {
                  console.error("User Denied access");
                }
                this.props.locationAccessDenied();
              },
              {
                //options
                enableHighAccuracy: true,
                maximumAge: 30 * 60 * 1000, //use if cache is not older than 30min
              }
            );
          } else {
            console.error("location could not be retrieved");

            this.props.locationAccessDenied();
          }
        }
      );
    }
  }

  fetchCurrentLocationAndReload() {
    if (!this.props.currentLocation && !this.props.isOwnerAtomUrisLoading) {
      if ("geolocation" in navigator) {
        navigator.geolocation.getCurrentPosition(
          currentLocation => {
            const lat = currentLocation.coords.latitude;
            const lng = currentLocation.coords.longitude;
            this.props.updateCurrentLocation(
              Immutable.fromJS({ location: { lat, lng } })
            );

            if (this.props.lastAtomUrisUpdateDate) {
              this.props.fetchWhatsAround(
                new Date(this.props.lastAtomUrisUpdateDate),
                { lat, lng },
                5000
              );
            } else {
              this.props.fetchWhatsAround(undefined, { lat, lng }, 5000);
            }
          },
          error => {
            //error handler
            console.error(
              "Could not retrieve geolocation due to error: ",
              error.code,
              ", continuing map initialization without currentLocation. fullerror:",
              error
            );
            if (error.code == 1) {
              console.error("User Denied access");
            }
            this.props.locationAccessDenied();
          },
          {
            //options
            enableHighAccuracy: true,
            maximumAge: 30 * 60 * 1000, //use if cache is not older than 30min
          }
        );
      } else {
        console.error("location could not be retrieved");
        this.props.locationAccessDenied();
      }
    }
  }

  selectLocation(location) {
    this.setState(
      {
        showLocationInput: false,
      },
      () => this.props.fetchWhatsAround(undefined, location, 5000)
    );
  }
}
PageMap.propTypes = {
  isLoggedIn: PropTypes.bool,
  currentLocation: PropTypes.object,
  isLocationAccessDenied: PropTypes.bool,
  lastWhatsAroundLocation: PropTypes.object,
  locations: PropTypes.arrayOf(PropTypes.object),
  lastAtomUrisUpdateDate: PropTypes.number,
  friendlyLastAtomUrisUpdateTimestamp: PropTypes.string,
  sortedVisibleAtomUriArray: PropTypes.arrayOf(PropTypes.string),
  hasVisibleAtomUris: PropTypes.bool,
  sortedVisibleAtomUriSize: PropTypes.number,
  isOwnerAtomUrisLoading: PropTypes.bool,
  isOwnerAtomUrisToLoad: PropTypes.bool,
  showSlideIns: PropTypes.bool,
  showModalDialog: PropTypes.bool,
  showConnectionOverlay: PropTypes.bool,
  viewConnUri: PropTypes.string,
  fetchWhatsAround: PropTypes.func,
  locationAccessDenied: PropTypes.func,
  updateCurrentLocation: PropTypes.func,
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(PageMap);
