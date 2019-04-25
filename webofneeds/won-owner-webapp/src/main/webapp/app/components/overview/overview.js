/**
 * Created by quasarchimaere on 04.04.2019.
 */
import angular from "angular";
import ngAnimate from "angular-animate";
import { attach, getIn, get, delay, sortByDate } from "../../utils.js";
import { connect2Redux } from "../../won-utils.js";
import won from "../../won-es6.js";
import { actionCreators } from "../../actions/actions.js";
import postMessagesModule from "../post-messages.js";
import atomCardModule from "../atom-card.js";
import postHeaderModule from "../post-header.js";
import * as generalSelectors from "../../selectors/general-selectors.js";
import * as viewSelectors from "../../selectors/view-selectors.js";
import * as processUtils from "../../process-utils.js";
import * as wonLabelUtils from "../../won-label-utils.js";
import * as atomUtils from "../../atom-utils.js";

import "style/_overview.scss";
import "style/_atom-overlay.scss";
import "style/_connection-overlay.scss";

const serviceDependencies = ["$ngRedux", "$scope"];
class Controller {
  constructor() {
    attach(this, serviceDependencies, arguments);
    this.selection = 0;
    window.overview4dbg = this;
    this.WON = won.WON;

    const selectFromState = state => {
      const viewAtomUri = generalSelectors.getViewAtomUriFromRoute(state);
      const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);

      const whatsNewMetaAtoms = getIn(state, ["owner", "metaAtoms"])
        .filter(metaAtom => atomUtils.isActive(metaAtom))
        .filter(metaAtom => !atomUtils.isSearchAtom(metaAtom))
        .filter(metaAtom => !atomUtils.isDirectResponseAtom(metaAtom))
        .filter(metaAtom => !atomUtils.isInvisibleAtom(metaAtom))
        .filter(
          (metaAtom, metaAtomUri) =>
            !generalSelectors.isAtomOwned(state, metaAtomUri)
        );

      const sortedVisibleAtoms = sortByDate(whatsNewMetaAtoms, "creationDate");
      const sortedVisibleAtomUriArray = sortedVisibleAtoms && [
        ...sortedVisibleAtoms.flatMap(visibleAtom => get(visibleAtom, "uri")),
      ];
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

      return {
        lastAtomUrisUpdateDate,
        friendlyLastAtomUrisUpdateTimestamp:
          lastAtomUrisUpdateDate &&
          wonLabelUtils.relativeTime(
            generalSelectors.selectLastUpdateTime(state),
            lastAtomUrisUpdateDate
          ),
        sortedVisibleAtomUriArray,
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
  .module("won.owner.components.overview", [
    ngAnimate,
    postMessagesModule,
    atomCardModule,
    postHeaderModule,
  ])
  .controller("OverviewController", Controller).name;
