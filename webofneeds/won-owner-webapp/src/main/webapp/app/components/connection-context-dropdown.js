/**
 * Created by ksinger on 30.03.2017.
 */

import angular from "angular";
import ngAnimate from "angular-animate";
import { actionCreators } from "../actions/actions.js";
import { attach, get, getIn, toAbsoluteURL } from "../utils.js";
import {
  getConnectionUriFromRoute,
  getOwnedNeedByConnectionUri,
} from "../selectors/general-selectors.js";
import { connect2Redux } from "../won-utils.js";
import { ownerBaseUrl } from "config";
import * as connectionUtils from "../connection-utils.js";
import * as needUtils from "../need-utils.js";
import * as processUtils from "../process-utils.js";

import "style/_context-dropdown.scss";

const serviceDependencies = ["$scope", "$ngRedux", "$element"];
function genComponentConf() {
  let template = `
            <svg class="cdd__icon__small"
                ng-if="self.connectionLoading"
                style="--local-primary:var(--won-skeleton-color);">
                    <use xlink:href="#ico16_contextmenu" href="#ico16_contextmenu"></use>
            </svg>
            <svg class="cdd__icon__small clickable"
                ng-if="!self.connectionLoading"
                style="--local-primary:var(--won-secondary-color);"
                ng-click="self.contextMenuOpen = true">
                    <use xlink:href="#ico16_contextmenu" href="#ico16_contextmenu"></use>
            </svg>
            <div class="cdd__contextmenu" ng-show="self.contextMenuOpen">
                <div class="cdd__contextmenu__content" ng-click="self.contextMenuOpen = false">
                    <div class="topline">
                        <svg class="cdd__icon__small__contextmenu clickable"
                            style="--local-primary:black;">
                            <use  xlink:href="#ico16_contextmenu" href="#ico16_contextmenu"></use>
                        </svg>
                    </div>
                    <!-- Buttons when connection is available -->
                    <button
                        class="won-button--outlined thin red"
                        ng-if="!self.isSuggested"
                        ng-click="self.goToPost(self.connection.get('remoteNeedUri'))">
                        Show Details
                    </button>
                    <button
                        ng-if="!self.isConnectionToGroup && self.isConnected && !self.showAgreementData"
                        class="won-button--outlined thin red"
                        ng-click="self.showAgreementDataField()">
                        Show Agreement Data
                    </button>
                    <button
                        ng-if="!self.isConnectionToGroup && self.isConnected && !self.showPetriNetData"
                        class="won-button--outlined thin red"
                        ng-click="self.showPetriNetDataField()">
                        Show PetriNet Data
                    </button>
                    <button
                        class="won-button--outlined thin red"
                        ng-if="self.isRemoteNeedUsableAsTemplate"
                        ng-click="self.router__stateGoAbs('connections', {fromNeedUri: self.remoteNeedUri, mode: 'DUPLICATE'})">
                        Post this too!
                    </button>
                    <a class="won-button--outlined thin red"
                        ng-if="self.adminEmail"
                        href="mailto:{{ self.adminEmail }}?{{ self.generateReportPostMailParams()}}">
                        Report Post
                    </a>
                    <button
                        class="won-button--filled red"
                        ng-click="self.closeConnection()">
                        {{ self.generateCloseConnectionLabel() }}
                    </button>
                </div>
            </div>
        `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const connectionUri = getConnectionUriFromRoute(state);

        const post =
          connectionUri && getOwnedNeedByConnectionUri(state, connectionUri);
        const connection = post && post.getIn(["connections", connectionUri]);

        const remoteNeedUri = getIn(connection, ["remoteNeedUri"]);
        const remoteNeed = getIn(state, ["needs", remoteNeedUri]);

        let linkToPost;
        if (ownerBaseUrl && remoteNeedUri) {
          const path = "#!post/" + `?postUri=${encodeURI(remoteNeedUri)}`;

          linkToPost = toAbsoluteURL(ownerBaseUrl).toString() + path;
        }
        const process = get(state, "process");

        return {
          connection,
          connectionUri,
          adminEmail: getIn(state, ["config", "theme", "adminEmail"]),
          remoteNeedUri,
          linkToPost,
          isConnectionToGroup: connectionUtils.isChatToGroup(
            state.get("needs"),
            get(post, "uri"),
            connectionUri
          ),
          showAgreementData: connection && connection.get("showAgreementData"),
          isConnected: connectionUtils.isConnected(connection),
          isSentRequest: connectionUtils.isRequestSent(connection),
          isReceivedRequest: connectionUtils.isRequestReceived(connection),
          isSuggested: connectionUtils.isSuggested(connection),
          isRemoteNeedUsableAsTemplate: needUtils.isUsableAsTemplate(
            remoteNeed
          ),
          connectionLoading:
            !connection ||
            processUtils.isConnectionLoading(process, connectionUri),
        };
      };
      connect2Redux(selectFromState, actionCreators, [], this);

      const callback = event => {
        const clickedElement = event.target;
        //hide MainMenu if click was outside of the component and menu was open
        if (
          this.contextMenuOpen &&
          !this.$element[0].contains(clickedElement)
        ) {
          this.contextMenuOpen = false;
          this.$scope.$apply();
        }
      };

      this.$scope.$on("$destroy", () => {
        window.document.removeEventListener("click", callback);
      });

      window.document.addEventListener("click", callback);
    }

    generateReportPostMailParams() {
      const subject = `[Report Post] - ${this.remoteNeedUri}`;
      const body = `Link to Post: ${this.linkToPost}%0D%0AReason:%0D%0A`; //hint: %0D%0A adds a linebreak

      return `subject=${subject}&body=${body}`;
    }

    closeConnection() {
      this.connections__close(this.connectionUri);
      this.router__stateGoCurrent({
        useCase: undefined,
        connectionUri: undefined,
      });
    }

    goToPost(postUri) {
      this.router__stateGoCurrent({ useCase: undefined, viewNeedUri: postUri });
    }

    generateCloseConnectionLabel() {
      if (this.isConnected) {
        return "Close Connection";
      } else if (this.isSuggested) {
        return "Remove Connection";
      } else if (this.isSentRequest) {
        return "Cancel Request";
      } else if (this.isReceivedRequest) {
        return "Deny Request";
      }
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      showAgreementDataField: "&",
      showPetriNetDataField: "&",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.connectionContextDropdown", [ngAnimate])
  .directive("wonConnectionContextDropdown", genComponentConf).name;
