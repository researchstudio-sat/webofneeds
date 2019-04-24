/**
 * Created by quasarchimaere on 04.04.2019.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import { attach, getIn, get, delay } from "../../utils.js";
import { connect2Redux } from "../../won-utils.js";
import won from "../../won-es6.js";
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
    this.WON = won.WON;

    const selectFromState = state => {
      const viewAtomUri = generalSelectors.getViewAtomUriFromRoute(state);
      const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);

      const atoms = getIn(state, ["owner", "atoms"]);
      const atomUris = getIn(state, ["owner", "atomUris"]);
      const lastAtomUrisUpdateDate = getIn(state, [
        "owner",
        "lastAtomsUpdateTime",
      ]);

      const process = get(state, "process");
      const isOwnerAtomUrisLoading = processUtils.isProcessingAtomUrisFromOwnerLoad(
        process
      );
      const isOwnerAtomUrisToLoad =
        !lastAtomUrisUpdateDate && !isOwnerAtomUrisLoading;

      const atomsWithLocation = getIn(state, ["atoms"]).filter(atom =>
        atomUtils.hasLocation(atom)
      );

      let locations = [];
      atomsWithLocation &&
        atomsWithLocation.map(atom => {
          const atomLocation = atomUtils.getLocation(atom);
          locations.push(atomLocation);
        });

      return {
        atoms,
        locations,
        atomsWithLocation,
        lastAtomUrisUpdateDate,
        friendlyLastAtomUrisUpdateTimestamp:
          lastAtomUrisUpdateDate &&
          wonLabelUtils.relativeTime(
            generalSelectors.selectLastUpdateTime(state),
            lastAtomUrisUpdateDate
          ),
        atomUrisArray: atomUris && atomUris.toArray().splice(0, 200), //FIXME: CURRENTLY LIMIT TO 200 entries
        atomUrisSize: atomUris ? atomUris.size : 0,
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
  }

  ensureAtomUrisLoaded() {
    if (this.isOwnerAtomUrisToLoad) {
      this.atoms__loadAllActiveAtomUrisFromOwner();
    }
  }

  reload() {
    if (!this.isOwnerAtomUrisLoading) {
      this.atoms__loadAllActiveAtomUrisFromOwner();
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