/**
 * Created by ksinger on 30.03.2017.
 */

import angular from "angular";
import ngAnimate from "angular-animate";
import { actionCreators } from "../actions/actions.js";
import won from "../won-es6.js";
import { attach, getIn, toAbsoluteURL } from "../utils.js";
import {
  getConnectionUriFromRoute,
  getOwnedNeedByConnectionUri,
} from "../selectors/general-selectors.js";
import { connect2Redux } from "../won-utils.js";
import { ownerBaseUrl } from "config";

import "style/_context-dropdown.scss";

const serviceDependencies = ["$scope", "$ngRedux", "$element"];
function genComponentConf() {
  let template = `
            <svg class="cdd__icon__small"
                ng-if="self.isLoading()"
                style="--local-primary:var(--won-skeleton-color);">
                    <use xlink:href="#ico16_contextmenu" href="#ico16_contextmenu"></use>
            </svg>
            <svg class="cdd__icon__small clickable"
                ng-if="!self.isLoading()"
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
                    <button class="won-button--outlined thin red show-in-responsive"
                        ng-click="self.toggleRdfDisplay()">
                        <svg class="won-button-icon" style="--local-primary:var(--won-primary-color);">
                            <use xlink:href="#ico36_rdf_logo" href="#ico36_rdf_logo"></use>
                        </svg>
                        <span>{{self.shouldShowRdf? "Hide raw RDF data" : "Show raw RDF data"}}</span>
                    </button>
                    <button
                        ng-if="self.isConnected && !self.showAgreementData"
                        class="won-button--outlined thin red"
                        ng-click="self.showAgreementDataField()">
                        Show Agreement Data
                    </button>
                    <button
                        ng-if="self.isConnected && !self.showPetriNetData"
                        class="won-button--outlined thin red"
                        ng-click="self.showPetriNetDataField()">
                        Show PetriNet Data
                    </button>
                    <a class="won-button--outlined thin red"
                        ng-if="self.adminEmail"
                        href="mailto:{{ self.adminEmail }}?{{ self.generateReportPostMailParams()}}">
                        Report Post
                    </a>
                    <button
                        ng-if="self.isConnected || self.isSuggested"
                        class="won-button--filled red"
                        ng-click="self.closeConnection()">
                        Remove Connection
                    </button>
                    <button
                        ng-if="self.isSentRequest"
                        class="won-button--filled red"
                        ng-click="self.closeConnection()">
                        Cancel Request
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
        const connectionState = connection && connection.get("state");

        const remotePostUri = getIn(connection, ["remoteNeedUri"]);

        let linkToPost;
        if (ownerBaseUrl && remotePostUri) {
          const path = "#!post/" + `?postUri=${encodeURI(remotePostUri)}`;

          linkToPost = toAbsoluteURL(ownerBaseUrl).toString() + path;
        }

        return {
          connection,
          connectionUri,
          adminEmail: getIn(state, ["config", "theme", "adminEmail"]),
          remotePostUri,
          linkToPost,
          showAgreementData: connection && connection.get("showAgreementData"),
          shouldShowRdf: state.get("showRdf"),
          isConnected: connectionState === won.WON.Connected,
          isSentRequest: connectionState === won.WON.RequestSent,
          isReceivedRequest: connectionState === won.WON.RequestReceived,
          isSuggested: connectionState === won.WON.Suggested,
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

    isLoading() {
      return !this.connection || this.connection.get("isLoading");
    }

    generateReportPostMailParams() {
      const subject = `[Report Post] - ${this.remotePostUri}`;
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
      this.router__stateGoCurrent({ useCase: undefined, postUri: postUri });
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
