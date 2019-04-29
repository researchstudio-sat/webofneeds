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
} from "../../utils.js";
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

import "style/_map.scss";
import "style/_atom-overlay.scss";
import "style/_connection-overlay.scss";

const serviceDependencies = ["$ngRedux", "$scope"];
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
      () => this.isOwnerAtomUrisToLoad,
      () => delay(0).then(() => this.ensureAtomUrisLoaded())
    );

    this.$scope.$watch(
      () => this.lastWhatsAroundLocation,
      () => delay(0).then(() => this.getLastWhatsAroundLocationName())
    );
  }

  ensureAtomUrisLoaded() {
    if (this.isOwnerAtomUrisToLoad && this.lastWhatsAroundLocation) {
      this.atoms__fetchWhatsAround(
        undefined,
        this.lastWhatsAroundLocation,
        5000
      );
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
          this.lastWhatsAroundLocationName = displayName;
        });
      });
    }
  }

  reload() {
    if (!this.isOwnerAtomUrisLoading) {
      if ("geolocation" in navigator) {
        navigator.geolocation.getCurrentPosition(
          currentLocation => {
            const lat = currentLocation.coords.latitude;
            const lng = currentLocation.coords.longitude;

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
            console.error("LOCATION COULD NOT BE RETRIEVED");
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
        console.error("LOCATION COULD NOT BE RETRIEVED");
        this.view__locationAccessDenied();
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
  ])
  .controller("MapController", Controller).name;
