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
import needCardModule from "../need-card.js";
import postHeaderModule from "../post-header.js";
import * as generalSelectors from "../../selectors/general-selectors.js";
import * as viewSelectors from "../../selectors/view-selectors.js";
import * as processUtils from "../../process-utils.js";
import * as srefUtils from "../../sref-utils.js";
import * as wonLabelUtils from "../../won-label-utils.js";

import "style/_overview.scss";
import "style/_need-overlay.scss";
import "style/_connection-overlay.scss";

const serviceDependencies = ["$ngRedux", "$scope"];
class Controller {
  constructor() {
    attach(this, serviceDependencies, arguments);
    this.selection = 0;
    window.overview4dbg = this;
    this.WON = won.WON;
    Object.assign(this, srefUtils); // bind srefUtils to scope

    const selectFromState = state => {
      const viewNeedUri = generalSelectors.getViewNeedUriFromRoute(state);
      const viewConnUri = generalSelectors.getViewConnectionUriFromRoute(state);

      const needUris = getIn(state, ["owner", "needUris"]);
      const lastNeedUrisUpdateDate = getIn(state, [
        "owner",
        "lastNeedUrisUpdateTime",
      ]);

      const process = get(state, "process");
      const isOwnerNeedUrisLoading = processUtils.isProcessingNeedUrisFromOwnerLoad(
        process
      );
      const isOwnerNeedUrisToLoad =
        !lastNeedUrisUpdateDate && !isOwnerNeedUrisLoading;

      return {
        lastNeedUrisUpdateDate,
        friendlyLastNeedUrisUpdateTimestamp:
          lastNeedUrisUpdateDate &&
          wonLabelUtils.relativeTime(
            generalSelectors.selectLastUpdateTime(state),
            lastNeedUrisUpdateDate
          ),
        needUrisArray: needUris && needUris.toArray().splice(0, 1000), //FIXME: CURRENTLY LIMIT TO 1000 entries
        needUrisSize: needUris ? needUris.size : 0,
        isOwnerNeedUrisLoading,
        isOwnerNeedUrisToLoad,
        showSlideIns:
          viewSelectors.hasSlideIns(state) && viewSelectors.showSlideIns(state),
        showModalDialog: viewSelectors.showModalDialog(state),
        showNeedOverlay: !!viewNeedUri,
        showConnectionOverlay: !!viewConnUri,
        viewNeedUri,
        viewConnUri,
      };
    };

    connect2Redux(selectFromState, actionCreators, [], this);

    this.$scope.$watch(
      () => this.isOwnerNeedUrisToLoad,
      () => delay(0).then(() => this.ensureNeedUrisLoaded())
    );
  }

  ensureNeedUrisLoaded() {
    if (this.isOwnerNeedUrisToLoad) {
      this.needs__loadAllActiveNeedUrisFromOwner();
    }
  }

  reload() {
    if (!this.isOwnerNeedUrisLoading) {
      this.needs__loadAllActiveNeedUrisFromOwner();
    }
  }
}

Controller.$inject = serviceDependencies;

export default angular
  .module("won.owner.components.overview", [
    ngAnimate,
    postMessagesModule,
    needCardModule,
    postHeaderModule,
  ])
  .controller("OverviewController", Controller).name;
