/**
 * Created by quasarchimaere on 04.04.2019.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import { attach, get, sortByDate } from "../../utils.js";
import { connect2Redux } from "../../won-utils.js";
import { actionCreators } from "../../actions/actions.js";
import postMessagesModule from "../post-messages.js";
import atomCardModule from "../atom-card.js";
import postHeaderModule from "../post-header.js";
import * as generalSelectors from "../../selectors/general-selectors.js";
import * as viewSelectors from "../../selectors/view-selectors.js";
import * as atomUtils from "../../atom-utils.js";

import "style/_inventory.scss";
import "style/_atom-overlay.scss";
import "style/_connection-overlay.scss";

const serviceDependencies = ["$ngRedux", "$scope"];
class Controller {
  constructor() {
    attach(this, serviceDependencies, arguments);
    this.selection = 0;
    window.inventory4dbg = this;

    const selectFromState = state => {
      const viewAtomUri = generalSelectors.getViewAtomUriFromRoute(state);
      const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);

      const ownedAtoms = generalSelectors.getOwnedAtoms(state);
      const ownedActiveAtoms = ownedAtoms.filter(atom =>
        atomUtils.isActive(atom)
      );
      const ownedInactiveAtoms = ownedAtoms.filter(atom =>
        atomUtils.isInactive(atom)
      );

      const sortedOwnedActiveAtoms = sortByDate(
        ownedActiveAtoms,
        "creationDate"
      );
      const sortedOwnedActiveAtomUriArray = sortedOwnedActiveAtoms && [
        ...sortedOwnedActiveAtoms.flatMap(atom => get(atom, "uri")),
      ];

      const sortedOwnedInactiveAtoms = sortByDate(
        ownedInactiveAtoms,
        "creationDate"
      );
      const sortedOwnedInactiveAtomUriArray = sortedOwnedInactiveAtoms && [
        ...sortedOwnedInactiveAtoms.flatMap(atom => get(atom, "uri")),
      ];

      return {
        currentLocation: generalSelectors.getCurrentLocation(state),
        sortedOwnedActiveAtomUriArray,
        sortedOwnedInactiveAtomUriArray,
        hasOwnedActiveAtomUris:
          sortedOwnedActiveAtomUriArray &&
          sortedOwnedActiveAtomUriArray.length > 0,
        hasOwnedInactiveAtomUris:
          sortedOwnedInactiveAtomUriArray &&
          sortedOwnedInactiveAtomUriArray.length > 0,
        sortedOwnedActiveAtomUriSize: sortedOwnedActiveAtomUriArray
          ? sortedOwnedActiveAtomUriArray.length
          : 0,
        sortedOwnedInactiveAtomUriSize: sortedOwnedInactiveAtomUriArray
          ? sortedOwnedInactiveAtomUriArray.length
          : 0,
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
  }
}

Controller.$inject = serviceDependencies;

export default angular
  .module("won.owner.components.inventory", [
    ngAnimate,
    postMessagesModule,
    atomCardModule,
    postHeaderModule,
  ])
  .controller("InventoryController", Controller).name;
