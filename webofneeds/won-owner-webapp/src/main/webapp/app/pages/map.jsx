/** @jsx h */
import angular from "angular";
import ngAnimate from "angular-animate";
import { delay, get, getIn } from "../utils.js";
import {
  reverseSearchNominatim,
  scrubSearchResults,
  searchNominatim,
} from "../api/nominatim-api.js";
import { attach, classOnComponentRoot } from "../cstm-ng-utils.js";
import Immutable from "immutable";
import { connect2Redux } from "../configRedux.js";
import { actionCreators } from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as viewSelectors from "../redux/selectors/view-selectors.js";
import * as processUtils from "../redux/utils/process-utils.js";
import * as wonLabelUtils from "../won-label-utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import wonInput from "../directives/input.js";
import { h } from "preact";
import * as accountUtils from "../redux/utils/account-utils.js";
import WonAtomCardGrid from "../components/atom-card-grid.jsx";
import WonAtomMap from "../components/atom-map.jsx";
import WonAtomMessages from "../components/atom-messages.jsx";

import "~/style/_map.scss";
import "~/style/_connection-overlay.scss";

const template = (
  <container>
    <won-modal-dialog ng-if="self.showModalDialog" />
    <div
      className="won-modal-connectionview"
      ng-if="self.showConnectionOverlay"
    >
      <won-preact
        component="self.WonAtomMessages"
        props="{connectionUri: self.viewConnUri}"
        className="atomMessages"
      />
    </div>
    <won-topnav page-title="::'What\'s around'" />
    <won-menu ng-if="self.isLoggedIn" />
    <won-toasts />
    <won-slide-in ng-if="self.showSlideIns" />
    <main className="ownermap">
      <div
        className="ownermap__header"
        ng-if="self.isLocationAccessDenied || self.lastWhatsAroundLocation"
      >
        <span className="ownermap__header__label">{"What's around:"}</span>
        <div
          className="ownermap__header__location"
          ng-click="self.showLocationInput = true"
          ng-show="!self.showLocationInput"
        >
          <svg className="ownermap__header__location__icon">
            <use
              xlinkHref="#ico36_detail_location"
              href="#ico36_detail_location"
            />
          </svg>
          <span className="ownermap__header__location__label">
            {"{{self.lastWhatsAroundLocationName}}"}
          </span>
        </div>
        <div
          className="ownermap__header__input"
          ng-show="self.showLocationInput || (self.isLocationAccessDenied && !self.lastWhatsAroundLocation)"
        >
          <svg
            className="ownermap__header__input__icon"
            ng-click="self.showLocationInput = false"
          >
            <use
              xlinkHref="#ico36_detail_location"
              href="#ico36_detail_location"
            />
          </svg>
          <input
            type="text"
            className="ownermap__header__input__inner"
            placeholder="Search around location"
            won-input="::self.updateWhatsAroundSuggestions()"
          />
          <svg
            className="ownermap__header__input__reset clickable"
            ng-if="self.showResetButton"
            ng-click="self.resetWhatsAroundInput()"
          >
            <use xlinkHref="#ico36_close" href="#ico36_close" />
          </svg>
        </div>
        <div className="ownermap__header__updated">
          <div
            className="ownermap__header__updated__loading hide-in-responsive"
            ng-if="self.isOwnerAtomUrisLoading && !self.showLocationInput"
          >
            Loading...
          </div>
          <div
            className="ownermap__header__updated__time hide-in-responsive"
            ng-if="!self.isOwnerAtomUrisLoading && !self.showLocationInput"
          >
            Updated: {"{{ self.friendlyLastAtomUrisUpdateTimestamp }}"}
          </div>
          <div
            className="ownermap__header__updated__reload won-button--filled red"
            ng-click="self.reload()"
            ng-disabled="self.isOwnerAtomUrisLoading"
            ng-if="!self.showLocationInput"
          >
            Reload
          </div>
          <div
            className="ownermap__header__updated__cancel won-button--filled red"
            ng-click="self.showLocationInput = false"
            ng-disabled="self.isOwnerAtomUrisLoading"
            ng-if="self.showLocationInput"
          >
            Cancel
          </div>
        </div>
      </div>
      <div
        className="ownermap__searchresults"
        ng-class="{'ownermap__searchresults--visible': self.showLocationInput || (self.isLocationAccessDenied && !self.lastWhatsAroundLocation)}"
        ng-if="!self.isOwnerAtomUrisToLoad || self.isLocationAccessDenied"
      >
        <div
          className="ownermap__searchresults__result"
          ng-if="!self.isLocationAccessDenied"
          ng-click="self.selectCurrentLocation()"
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
        <div
          className="ownermap__searchresults__result"
          ng-repeat="result in self.searchResults"
          ng-click="self.selectLocation(result)"
        >
          <svg className="ownermap__searchresults__result__icon">
            <use
              xlinkHref="#ico16_indicator_location"
              href="#ico16_indicator_location"
            />
          </svg>
          <div className="ownermap__searchresults__result__label">
            {"{{ result.name }}"}
          </div>
        </div>
        <div
          className="ownermap__searchresults__deniedlocation"
          ng-if="self.isLocationAccessDenied && !self.lastWhatsAroundLocation && !self.hasVisibleAtomUris && !(self.searchResults && self.searchResults.length > 0)"
        >
          <svg className="ownermap__searchresults__deniedlocation__icon">
            <use
              xlinkHref="#ico16_indicator_error"
              href="#ico16_indicator_error"
            />
          </svg>
          <div className="ownermap__searchresults__deniedlocation__label">
            {`You prohibit us from retrieving your location, so we won't be able
            to show what's around you. If you want to change that, grant access
            to the location in your browser and reload the page, or type any
            location in the input-field above.`}
          </div>
        </div>
      </div>
      <div
        className="ownermap__nolocation"
        ng-if="!self.currentLocation && !self.isLocationAccessDenied && !self.lastWhatsAroundLocation"
      >
        <svg className="ownermap__nolocation__icon">
          <use
            xlinkHref="#ico36_detail_location"
            href="#ico36_detail_location"
          />
        </svg>
        <div className="ownermap__nolocation__label">
          You did not grant location access yet.{" "}
          <span className="show-in-responsive">Tap</span>
          <span className="hide-in-responsive">Click</span> the button below and
          accept the location access to see what is going on around you.
        </div>
        <div
          className="ownermap__nolocation__button won-button--filled red"
          ng-click="self.fetchCurrentLocationAndReload()"
        >
          {"See What's Around"}
        </div>
      </div>
      <won-preact
        className="ownermap__map hide-in-responsive won-atom-map"
        component="self.WonAtomMap"
        ng-class="{'ownermap__map--visible': !(self.showLocationInput || (self.isLocationAccessDenied && !self.lastWhatsAroundLocation))}"
        props="{ locations: self.locations, currentLocation: self.lastWhatsAroundLocation }"
        ng-if="!self.isOwnerAtomUrisToLoad && self.lastWhatsAroundLocation"
      />
      <div
        className="ownermap__content"
        ng-if="self.lastWhatsAroundLocation && self.hasVisibleAtomUris"
      >
        <won-preact
          component="self.WonAtomCardGrid"
          props="{ atomUris: self.sortedVisibleAtomUriArray, currentLocation: self.lastWhatsAroundLocation, showSuggestions: false, showPersona: true, showCreate: false }"
          ng-if="self.hasVisibleAtomUris"
        />
      </div>
      <div
        className="ownermap__noresults"
        ng-if="self.lastWhatsAroundLocation && !self.hasVisibleAtomUris"
      >
        <span className="ownermap__noresults__label">
          Nothing around this location, you can try another location by clicking
          on the location in the header.
        </span>
      </div>
    </main>
    <won-footer />
  </container>
);

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
class Controller {
  constructor() {
    attach(this, serviceDependencies, arguments);
    window.ownermap4dbg = this;
    this.WonAtomMap = WonAtomMap;
    this.WonAtomCardGrid = WonAtomCardGrid;
    this.WonAtomMessages = WonAtomMessages;

    const selectFromState = state => {
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
      const isOwnerAtomUrisLoading = processUtils.isProcessingWhatsAround(
        process
      );
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

    connect2Redux(selectFromState, actionCreators, [], this);
    classOnComponentRoot("won-signed-out", () => !this.isLoggedIn, this);
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

export default {
  module: angular
    .module("won.owner.components.map", [ngAnimate, wonInput])
    .controller("MapController", Controller).name,
  controller: "MapController",
  template: template,
};
