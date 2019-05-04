/**
 * Created by quasarchimaere on 04.04.2019.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import {
  attach,
  getIn,
  get,
  delay,
  reverseSearchNominatim,
  searchNominatim,
  scrubSearchResults,
} from "../../utils.js";
import Immutable from "immutable";
import { connect2Redux } from "../../won-utils.js";
import { actionCreators } from "../../actions/actions.js";
import postMessagesModule from "../post-messages.js";
import atomCardModule from "../atom-card.js";
import atomMapModule from "../atom-map.js";
import postHeaderModule from "../post-header.js";
import * as generalSelectors from "../../selectors/general-selectors.js";
import * as viewSelectors from "../../selectors/view-selectors.js";
import * as processUtils from "../../process-utils.js";
import * as wonLabelUtils from "../../won-label-utils.js";
import * as atomUtils from "../../atom-utils.js";
import wonInput from "../../directives/input.js";

import "style/_map.scss";
import "style/_atom-overlay.scss";
import "style/_connection-overlay.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
class Controller {
  constructor() {
    attach(this, serviceDependencies, arguments);
    this.selection = 0;
    window.ownermap4dbg = this;

    const selectFromState = state => {
      const viewAtomUri = generalSelectors.getViewAtomUriFromRoute(state);
      const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);

      const isLocationAccessDenied = generalSelectors.isLocationAccessDenied(
        state
      );
      const currentLocation = generalSelectors.getCurrentLocation(state);

      const lastWhatsAroundLocation = getIn(state, [
        "owner",
        "lastWhatsAroundLocation",
      ]);
      const whatsAroundMaxDistance = getIn(state, [
        "owner",
        "lastWhatsAroundMaxDistance",
      ]);

      const whatsNewMetaAtoms = getIn(state, ["owner", "whatsAround"])
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
        whatsNewMetaAtoms,
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
      const isOwnerAtomUrisLoading = processUtils.isProcessingWhatsAround(
        process
      );
      const isOwnerAtomUrisToLoad =
        !lastAtomUrisUpdateDate && !isOwnerAtomUrisLoading;

      let locations = [];
      whatsNewMetaAtoms &&
        whatsNewMetaAtoms.map(atom => {
          const atomLocation = atomUtils.getLocation(atom);
          locations.push(atomLocation);
        });

      return {
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
          viewSelectors.hasSlideIns(state) && viewSelectors.showSlideIns(state),
        showModalDialog: viewSelectors.showModalDialog(state),
        showAtomOverlay: !!viewAtomUri,
        showConnectionOverlay: !!viewConnUri,
        viewAtomUri,
        viewConnUri,
      };
    };

    connect2Redux(selectFromState, actionCreators, [], this);

    this.$scope.$watch(
      () => this.isOwnerAtomUrisToLoad && this.currentLocation,
      () => delay(0).then(() => this.ensureAtomUrisLoaded())
    );

    this.$scope.$watch(
      () => this.lastWhatsAroundLocation,
      () => delay(0).then(() => this.getLastWhatsAroundLocationName())
    );
  }

  ensureAtomUrisLoaded() {
    if (this.isOwnerAtomUrisToLoad && this.currentLocation) {
      const latlng = {
        lat: this.currentLocation.get("lat"),
        lng: this.currentLocation.get("lng"),
      };
      this.atoms__fetchWhatsAround(undefined, latlng, 5000);
    }
  }

  getLastWhatsAroundLocationName() {
    if (this.lastWhatsAroundLocation) {
      reverseSearchNominatim(
        this.lastWhatsAroundLocation.get("lat"),
        this.lastWhatsAroundLocation.get("lng"),
        13
      ).then(searchResult => {
        const displayName = searchResult.display_name;
        this.$scope.$apply(() => {
          this.whatsAroundInput().value = displayName;
          this.showResetButton = !!displayName && displayName.length > 0;
          this.lastWhatsAroundLocationName = displayName;
        });
      });
    }
  }

  selectLocation(location) {
    this.showLocationInput = false;
    this.atoms__fetchWhatsAround(undefined, location, 5000);
  }

  selectCurrentLocation() {
    if (!this.isLocationAccessDenied) {
      if ("geolocation" in navigator) {
        this.showLocationInput = false;
        navigator.geolocation.getCurrentPosition(
          currentLocation => {
            const lat = currentLocation.coords.latitude;
            const lng = currentLocation.coords.longitude;
            this.view__updateCurrentLocation(
              Immutable.fromJS({ location: { lat, lng } })
            );

            if (this.lastAtomUrisUpdateDate) {
              this.atoms__fetchWhatsAround(
                new Date(this.lastAtomUrisUpdateDate),
                { lat, lng },
                5000
              );
            } else {
              this.atoms__fetchWhatsAround(undefined, { lat, lng }, 5000);
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
            this.view__locationAccessDenied();
          },
          {
            //options
            enableHighAccuracy: true,
            maximumAge: 30 * 60 * 1000, //use if cache is not older than 30min
          }
        );
      } else {
        console.error("location could not be retrieved");
        this.showLocationInput = false;
        this.view__locationAccessDenied();
      }
    }
  }

  updateWhatsAroundSuggestions() {
    const whatsAroundInputValue =
      !!this.whatsAroundInput().value && this.whatsAroundInput().value.trim();

    if (!!whatsAroundInputValue && whatsAroundInputValue.length > 0) {
      searchNominatim(whatsAroundInputValue).then(searchResults => {
        const parsedResults = scrubSearchResults(
          searchResults,
          whatsAroundInputValue
        );
        this.$scope.$apply(() => {
          this.showResetButton = true;
          this.searchResults = parsedResults;
        });
      });
    } else {
      this.resetWhatsAroundInput();
    }
  }

  resetWhatsAroundInput() {
    this.whatsAroundInput().value = "";
    this.showResetButton = false;
    this.searchResults = [];
  }

  whatsAroundInput() {
    if (!this._whatsAroundInput) {
      this._whatsAroundInput = this.$element[0].querySelector(
        ".ownermap__header__input__inner"
      );
    }
    return this._whatsAroundInput;
  }

  fetchCurrentLocationAndReload() {
    if (!this.currentLocation && !this.isOwnerAtomUrisLoading) {
      if ("geolocation" in navigator) {
        navigator.geolocation.getCurrentPosition(
          currentLocation => {
            const lat = currentLocation.coords.latitude;
            const lng = currentLocation.coords.longitude;
            this.view__updateCurrentLocation(
              Immutable.fromJS({ location: { lat, lng } })
            );

            if (this.lastAtomUrisUpdateDate) {
              this.atoms__fetchWhatsAround(
                new Date(this.lastAtomUrisUpdateDate),
                { lat, lng },
                5000
              );
            } else {
              this.atoms__fetchWhatsAround(undefined, { lat, lng }, 5000);
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
            this.view__locationAccessDenied();
          },
          {
            //options
            enableHighAccuracy: true,
            maximumAge: 30 * 60 * 1000, //use if cache is not older than 30min
          }
        );
      } else {
        console.error("location could not be retrieved");
        this.view__locationAccessDenied();
      }
    }
  }

  reload() {
    if (!this.isOwnerAtomUrisLoading && this.lastWhatsAroundLocation) {
      const latlng = {
        lat: this.lastWhatsAroundLocation.get("lat"),
        lng: this.lastWhatsAroundLocation.get("lng"),
      };
      if (this.lastAtomUrisUpdateDate) {
        this.atoms__fetchWhatsAround(
          new Date(this.lastAtomUrisUpdateDate),
          latlng,
          5000
        );
      } else {
        this.atoms__fetchWhatsAround(undefined, latlng, 5000);
      }
    }
  }
}

Controller.$inject = serviceDependencies;

export default angular
  .module("won.owner.components.map", [
    ngAnimate,
    postMessagesModule,
    atomMapModule,
    atomCardModule,
    postHeaderModule,
    wonInput,
  ])
  .controller("MapController", Controller).name;
