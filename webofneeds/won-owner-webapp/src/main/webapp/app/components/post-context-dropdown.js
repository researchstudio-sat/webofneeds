/**
 * Created by ksinger on 30.03.2017.
 */

import angular from "angular";
import ngAnimate from "angular-animate";
import { actionCreators } from "../actions/actions.js";
import { attach, toAbsoluteURL, getIn, get } from "../utils.js";
import {
  connect2Redux,
  createDocumentDefinitionFromPost,
} from "../won-utils.js";
import * as atomUtils from "../atom-utils.js";
import * as processUtils from "../process-utils.js";
import * as generalSelectors from "../selectors/general-selectors.js";
import pdfMake from "pdfmake/build/pdfmake";
import pdfFonts from "pdfmake/build/vfs_fonts";

import { ownerBaseUrl } from "~/config/default.js";

import "~/style/_context-dropdown.scss";

const serviceDependencies = ["$scope", "$ngRedux", "$element"];
function genComponentConf() {
  let template = `
            <svg class="cdd__icon__small"
                ng-if="self.postLoading || self.postFailedToLoad"
                style="--local-primary:var(--won-skeleton-color);">
                    <use xlink:href="#ico16_contextmenu" href="#ico16_contextmenu"></use>
            </svg>
            <svg class="cdd__icon__small clickable"
                ng-if="!self.postLoading && !self.postFailedToLoad"
                style="--local-primary:var(--won-secondary-color);"
                ng-click="self.contextMenuOpen = true">
                    <use xlink:href="#ico16_contextmenu" href="#ico16_contextmenu"></use>
            </svg>
            <div class="cdd__contextmenu" ng-show="self.contextMenuOpen">
                <div class="cdd__contextmenu__content" ng-click="self.contextMenuOpen = false">
                    <div class="topline">
                        <svg class="cdd__icon__small__contextmenu clickable"
                            style="--local-primary:black;">
                            <use xlink:href="#ico16_contextmenu" href="#ico16_contextmenu"></use>
                        </svg>
                    </div>
                    <!-- Buttons for post -->
                    <button class="won-button--outlined thin red"
                        ng-click="self.exportPdf()">
                        Export as PDF
                    </button>
                    <button
                        class="won-button--outlined thin red"
                        ng-if="self.isUsableAsTemplate"
                        ng-click="self.router__stateGoAbs('connections', {fromAtomUri: self.atomUri, mode: 'DUPLICATE'})">
                        Post this too!
                    </button>
                    <button
                        class="won-button--outlined thin red"
                        ng-if="self.isEditable"
                        ng-click="self.router__stateGoAbs('connections', {fromAtomUri: self.atomUri, mode: 'EDIT'})">
                        Edit
                    </button>
                    <a class="won-button--outlined thin red"
                        ng-if="self.adminEmail"
                        href="mailto:{{ self.adminEmail }}?{{ self.generateReportPostMailParams()}}">
                        Report
                    </a>
                    <button class="won-button--filled red"
                        ng-if="self.isOwnPost && self.isInactive"
                        ng-click="self.reOpenPost()">
                        Reopen
                    </button>
                    <button class="won-button--filled red"
                        ng-if="self.isOwnPost && self.isInactive"
                        ng-click="self.deletePost()">
                        Delete
                    </button>
                    <button class="won-button--filled red"
                        ng-if="self.isOwnPost && self.isActive"
                        ng-click="self.closePost()">
                        Remove
                    </button>
                </div>
            </div>
        `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const post = this.atomUri && state.getIn(["atoms", this.atomUri]);

        let linkToPost;
        if (ownerBaseUrl && post) {
          const path = "#!post/" + `?postUri=${encodeURI(post.get("uri"))}`;

          linkToPost = toAbsoluteURL(ownerBaseUrl).toString() + path;
        }

        const process = get(state, "process");

        return {
          adminEmail: getIn(state, ["config", "theme", "adminEmail"]),
          isOwnPost: generalSelectors.isAtomOwned(state, this.atomUri),
          isActive: atomUtils.isActive(post),
          isInactive: atomUtils.isInactive(post),
          isUsableAsTemplate: generalSelectors.isAtomUsableAsTemplate(
            state,
            this.atomUri
          ),
          isEditable: generalSelectors.isAtomEditable(state, this.atomUri),
          post,
          postLoading:
            !post || processUtils.isAtomLoading(process, post.get("uri")),
          postFailedToLoad:
            post && processUtils.hasAtomFailedToLoad(process, post.get("uri")),
          linkToPost,
        };
      };
      connect2Redux(selectFromState, actionCreators, ["self.atomUri"], this);

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

    closePost() {
      if (this.isOwnPost) {
        const payload = {
          caption: "Attention!",
          text:
            "Deleting or archiving the Post will close all connections, do you want to proceed?",
          buttons: [
            {
              caption: "Delete",
              callback: () => {
                /*this.atoms__delete(this.post.get("uri"));
                this.router__stateGoCurrent({
                  useCase: undefined,
                  postUri: undefined,
                });
                this.view__hideModalDialog();*/
                this.deletePost();
              },
            },
            {
              caption: "Archive",
              callback: () => {
                this.atoms__close(this.post.get("uri"));
                this.router__stateGoCurrent({
                  useCase: undefined,
                  postUri: undefined,
                });
                this.view__hideModalDialog();
              },
            },
            {
              caption: "Cancel",
              callback: () => {
                this.view__hideModalDialog();
              },
            },
          ],
        };
        this.view__showModalDialog(payload);
      }
    }

    deletePost() {
      if (this.isOwnPost) {
        const payload = {
          caption: "Attention!",
          text: "Deleting the Post is irreversible, do you want to proceed?",
          buttons: [
            {
              caption: "Yes",
              callback: () => {
                this.atoms__delete(this.post.get("uri"));
                this.router__stateGoCurrent({
                  useCase: undefined,
                  postUri: undefined,
                });
                this.view__hideModalDialog();
              },
            },
            {
              caption: "No",
              callback: () => {
                this.view__hideModalDialog();
              },
            },
          ],
        };
        this.view__showModalDialog(payload);
      }
    }

    generateReportPostMailParams() {
      const subject = `[Report Post] - ${this.atomUri}`;
      const body = `Link to Post: ${this.linkToPost}%0D%0AReason:%0D%0A`; //hint: %0D%0A adds a linebreak

      return `subject=${subject}&body=${body}`;
    }

    reOpenPost() {
      if (this.isOwnPost) {
        this.atoms__reopen(this.post.get("uri"));
      }
    }

    exportPdf() {
      if (!this.post) return;
      const docDefinition = createDocumentDefinitionFromPost(this.post);

      if (docDefinition) {
        pdfMake.vfs = pdfFonts.pdfMake.vfs;
        pdfMake.createPdf(docDefinition).download();
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
      atomUri: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.postContextDropdown", [ngAnimate])
  .directive("wonPostContextDropdown", genComponentConf).name;
