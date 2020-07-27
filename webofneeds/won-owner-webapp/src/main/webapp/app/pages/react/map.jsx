import React, { useState, useEffect, useRef } from "react";
import Immutable from "immutable";
import { useDispatch, useSelector } from "react-redux";
import { actionCreators } from "../../actions/actions.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import { getIn, get } from "../../utils.js";
import { usePrevious } from "../../cstm-react-utils.js";
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
import WonAtomMap from "../../components/atom-map.jsx";
import WonMenu from "../../components/menu.jsx";
import WonToasts from "../../components/toasts.jsx";
import WonSlideIn from "../../components/slide-in.jsx";
import WonAtomCardGrid from "../../components/atom-card-grid.jsx";
import WonFooter from "../../components/footer.jsx";
import WonTitlePicker from "../../components/details/picker/title-picker.jsx";

import _debounce from "lodash/debounce";

import "~/style/_map.scss";
import ico36_detail_location from "~/images/won-icons/ico36_detail_location.svg";
import ico36_location_current from "~/images/won-icons/ico36_location_current.svg";
import ico16_indicator_location from "~/images/won-icons/ico16_indicator_location.svg";
import ico16_indicator_error from "~/images/won-icons/ico16_indicator_error.svg";
import { useHistory } from "react-router-dom";

export default function PageMap() {
  const dispatch = useDispatch();
  const history = useHistory();

  const isLocationAccessDenied = useSelector(
    generalSelectors.isLocationAccessDenied
  );
  const currentLocation = useSelector(generalSelectors.getCurrentLocation);

  const [state, setState] = useState({
    searchText: "",
    searchResults: [],
    lastWhatsAroundLocationName: "",
  });
  const [showLocationInput, setShowLocationInput] = useState(false);

  const lastWhatsAroundLocation = useSelector(state =>
    getIn(state, ["owner", "lastWhatsAroundLocation"])
  );

  const previousLastWhatsAroundLocation = usePrevious(
    lastWhatsAroundLocation,
    useRef,
    useEffect
  );
  const whatsAroundMaxDistance = useSelector(state =>
    getIn(state, ["owner", "lastWhatsAroundMaxDistance"])
  );

  const debugModeEnabled = useSelector(viewSelectors.isDebugModeEnabled);

  const accountState = useSelector(generalSelectors.getAccountState);
  const whatsAroundMetaAtoms = useSelector(state =>
    generalSelectors
      .getWhatsAroundAtoms(state)
      .filter(metaAtom => atomUtils.isActive(metaAtom))
      .filter(
        metaAtom => debugModeEnabled || !atomUtils.isInvisibleAtom(metaAtom)
      )
      .filter(metaAtom => !accountUtils.isAtomOwned(metaAtom))
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
      })
      .toOrderedMap()
      .sortBy(metaAtom => atomUtils.getDistanceFrom(metaAtom, currentLocation))
  );

  const lastAtomUrisUpdateDate = useSelector(state =>
    getIn(state, ["owner", "lastWhatsAroundUpdateTime"])
  );

  const process = useSelector(generalSelectors.getProcessState);
  const isOwnerAtomUrisLoading = processUtils.isProcessingWhatsAround(process);
  const isOwnerAtomUrisToLoad =
    !lastAtomUrisUpdateDate && !isOwnerAtomUrisLoading;

  let locations = [];
  whatsAroundMetaAtoms &&
    whatsAroundMetaAtoms.map(atom => {
      const atomLocation = atomUtils.getLocation(atom);
      locations.push(atomLocation);
    });

  const isLoggedIn = accountUtils.isLoggedIn(accountState);
  const globalLastUpdateDate = useSelector(
    generalSelectors.selectLastUpdateTime
  );
  const friendlyLastAtomUrisUpdateTimestamp =
    lastAtomUrisUpdateDate &&
    wonLabelUtils.relativeTime(globalLastUpdateDate, lastAtomUrisUpdateDate);

  const hasVisibleAtoms = whatsAroundMetaAtoms && whatsAroundMetaAtoms.size > 0;
  const showSlideIns = useSelector(viewSelectors.showSlideIns(history));
  const showModalDialog = useSelector(viewSelectors.showModalDialog);

  const startSearch = _debounce(value => {
    searchNominatim(value).then(searchResults => {
      const parsedResults = scrubSearchResults(searchResults, value);

      setState({
        ...state,
        searchResults: parsedResults || [],
        lastWhatsAroundLocationName: value,
      });
    });
  }, 700);

  function updateWhatsAroundSuggestions({ value }) {
    const whatsAroundInputValue = value && value.trim();
    setState({ ...state, searchText: value });
    if (!!whatsAroundInputValue && whatsAroundInputValue.length > 0) {
      startSearch(value);
    } else {
      resetWhatsAroundInput();
    }
  }

  function resetWhatsAroundInput() {
    setState({
      ...state,
      searchText: "",
      searchResults: [],
    });
  }

  function reload() {
    if (!isOwnerAtomUrisLoading && lastWhatsAroundLocation) {
      const latlng = {
        lat: get(lastWhatsAroundLocation, "lat"),
        lng: get(lastWhatsAroundLocation, "lng"),
      };
      dispatch(
        actionCreators.atoms__fetchWhatsAround(
          lastAtomUrisUpdateDate ? new Date(lastAtomUrisUpdateDate) : undefined,
          latlng,
          5000
        )
      );
    }
  }

  function selectCurrentLocation() {
    if (!isLocationAccessDenied) {
      setShowLocationInput(false);
      if ("geolocation" in navigator) {
        navigator.geolocation.getCurrentPosition(
          currentLocation => {
            const lat = currentLocation.coords.latitude;
            const lng = currentLocation.coords.longitude;
            dispatch(
              actionCreators.view__updateCurrentLocation(
                Immutable.fromJS({ location: { lat, lng } })
              )
            );
            dispatch(
              actionCreators.atoms__fetchWhatsAround(
                lastAtomUrisUpdateDate
                  ? new Date(lastAtomUrisUpdateDate)
                  : undefined,
                { lat, lng },
                5000
              )
            );
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
            dispatch(actionCreators.view__locationAccessDenied());
          },
          {
            //options
            enableHighAccuracy: true,
            maximumAge: 30 * 60 * 1000, //use if cache is not older than 30min
          }
        );
      } else {
        console.error("location could not be retrieved");
        dispatch(actionCreators.view__locationAccessDenied());
      }
    }
  }

  function fetchCurrentLocationAndReload() {
    if (!currentLocation && !isOwnerAtomUrisLoading) {
      if ("geolocation" in navigator) {
        navigator.geolocation.getCurrentPosition(
          currentLocation => {
            const lat = currentLocation.coords.latitude;
            const lng = currentLocation.coords.longitude;
            dispatch(
              actionCreators.view__updateCurrentLocation(
                Immutable.fromJS({ location: { lat, lng } })
              )
            );
            dispatch(
              actionCreators.atoms__fetchWhatsAround(
                lastAtomUrisUpdateDate
                  ? new Date(lastAtomUrisUpdateDate)
                  : undefined,
                { lat, lng },
                5000
              )
            );
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
            dispatch(actionCreators.view__locationAccessDenied());
          },
          {
            //options
            enableHighAccuracy: true,
            maximumAge: 30 * 60 * 1000, //use if cache is not older than 30min
          }
        );
      } else {
        console.error("location could not be retrieved");
        dispatch(actionCreators.view__locationAccessDenied());
      }
    }
  }

  function selectLocation(location) {
    setShowLocationInput(false);
    dispatch(actionCreators.atoms__fetchWhatsAround(undefined, location, 5000));
  }

  useEffect(() => {
    if (
      lastWhatsAroundLocation &&
      lastWhatsAroundLocation !== previousLastWhatsAroundLocation
    ) {
      reverseSearchNominatim(
        get(lastWhatsAroundLocation, "lat"),
        get(lastWhatsAroundLocation, "lng"),
        13
      ).then(searchResult => {
        const displayName = searchResult.display_name;

        setState({
          ...state,
          searchText: displayName,
          lastWhatsAroundLocationName: displayName,
        });
      });
    }

    if (isOwnerAtomUrisToLoad && currentLocation) {
      const latlng = {
        lat: get(currentLocation, "lat"),
        lng: get(currentLocation, "lng"),
      };
      dispatch(actionCreators.atoms__fetchWhatsAround(undefined, latlng, 5000));
    }
  });

  return (
    <section className={!isLoggedIn ? "won-signed-out" : ""}>
      {showModalDialog && <WonModalDialog />}
      <WonTopnav pageTitle="What's around" />
      {isLoggedIn && <WonMenu />}
      <WonToasts />
      {showSlideIns && <WonSlideIn />}
      <main className="ownermap">
        {(isLocationAccessDenied || lastWhatsAroundLocation) && (
          <div className="ownermap__header">
            <span className="ownermap__header__label">{"What's around:"}</span>
            {!showLocationInput && (
              <div
                className="ownermap__header__location"
                onClick={() => setShowLocationInput(true)}
              >
                <svg className="ownermap__header__location__icon">
                  <use
                    xlinkHref={ico36_detail_location}
                    href={ico36_detail_location}
                  />
                </svg>
                <span className="ownermap__header__location__label">
                  {state.lastWhatsAroundLocationName}
                </span>
              </div>
            )}
            {(showLocationInput ||
              (isLocationAccessDenied && !lastWhatsAroundLocation)) && (
              <div className="ownermap__header__input">
                <svg
                  className="ownermap__header__input__icon"
                  onClick={() => setShowLocationInput(false)}
                >
                  <use
                    xlinkHref={ico36_detail_location}
                    href={ico36_detail_location}
                  />
                </svg>
                <WonTitlePicker
                  onUpdate={updateWhatsAroundSuggestions}
                  initialValue={state.searchText}
                  detail={{ placeholder: "Search around location" }}
                />
              </div>
            )}

            <div className="ownermap__header__updated">
              {!showLocationInput &&
                (isOwnerAtomUrisLoading ? (
                  <div className="ownermap__header__updated__loading">
                    Loading...
                  </div>
                ) : (
                  <div className="ownermap__header__updated__time">
                    {"Updated: " + friendlyLastAtomUrisUpdateTimestamp}
                  </div>
                ))}

              {showLocationInput ? (
                <div
                  className="ownermap__header__updated__cancel won-button--filled secondary"
                  onClick={() => setShowLocationInput(false)}
                  disabled={isOwnerAtomUrisLoading}
                >
                  Cancel
                </div>
              ) : (
                <div
                  className="ownermap__header__updated__reload won-button--filled secondary"
                  onClick={reload}
                  disabled={isOwnerAtomUrisLoading}
                >
                  Reload
                </div>
              )}
            </div>
          </div>
        )}
        {(!isOwnerAtomUrisToLoad || isLocationAccessDenied) && (
          <div
            className={
              "ownermap__searchresults " +
              (showLocationInput ||
              (isLocationAccessDenied && !lastWhatsAroundLocation)
                ? "ownermap__searchresults--visible"
                : "")
            }
          >
            {!isLocationAccessDenied && (
              <div
                className="ownermap__searchresults__result"
                onClick={selectCurrentLocation}
              >
                <svg className="ownermap__searchresults__result__icon">
                  <use
                    xlinkHref={ico36_location_current}
                    href={ico36_location_current}
                  />
                </svg>
                <div className="ownermap__searchresults__result__label">
                  Current Location
                </div>
              </div>
            )}
            {state.searchResults.map((result, index) => (
              <div
                className="ownermap__searchresults__result"
                onClick={() => selectLocation(result)}
                key={result.name + "-" + index}
              >
                <svg className="ownermap__searchresults__result__icon">
                  <use
                    xlinkHref={ico16_indicator_location}
                    href={ico16_indicator_location}
                  />
                </svg>
                <div className="ownermap__searchresults__result__label">
                  {result.name}
                </div>
              </div>
            ))}

            {isLocationAccessDenied &&
              !lastWhatsAroundLocation &&
              !hasVisibleAtoms &&
              !(state.searchResults.length > 0) && (
                <div className="ownermap__searchresults__deniedlocation">
                  <svg className="ownermap__searchresults__deniedlocation__icon">
                    <use
                      xlinkHref={ico16_indicator_error}
                      href={ico16_indicator_error}
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
        {!currentLocation &&
          !isLocationAccessDenied &&
          !lastWhatsAroundLocation && (
            <div className="ownermap__nolocation">
              <svg className="ownermap__nolocation__icon">
                <use
                  xlinkHref={ico36_detail_location}
                  href={ico36_detail_location}
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
                className="ownermap__nolocation__button won-button--filled secondary"
                onClick={fetchCurrentLocationAndReload}
              >
                {"See What's Around"}
              </div>
            </div>
          )}

        {!isOwnerAtomUrisToLoad && !!lastWhatsAroundLocation ? (
          <WonAtomMap
            className={
              "ownermap__map won-atom-map " +
              (!(
                showLocationInput ||
                (isLocationAccessDenied && !lastWhatsAroundLocation)
              )
                ? " ownermap__map--visible "
                : "")
            }
            locations={locations}
            currentLocation={lastWhatsAroundLocation}
          />
        ) : (
          undefined
        )}
        {lastWhatsAroundLocation &&
          (hasVisibleAtoms ? (
            <div className="ownermap__content">
              <WonAtomCardGrid
                atoms={whatsAroundMetaAtoms}
                currentLocation={lastWhatsAroundLocation}
                showHolder={true}
                showIndicators={false}
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
