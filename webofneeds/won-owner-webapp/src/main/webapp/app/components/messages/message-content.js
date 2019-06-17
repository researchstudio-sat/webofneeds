import angular from "angular";

import won from "../../won-es6.js";
import { connect2Redux } from "../../won-utils.js";
import * as useCaseUtils from "../../usecase-utils.js";
import { attach, getIn, get } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { getOwnedAtomByConnectionUri } from "../../redux/selectors/general-selectors.js";
import "angular-marked";

import "~/style/_message-content.scss";
import "~/style/_won-markdown.scss";

const serviceDependencies = ["$ngRedux", "$scope"];

function genComponentConf() {
  let template = `
      <div class="msg__text markdown" ng-if="self.message && self.text" marked="self.text"></div>
      <div class="msg__content"
        ng-repeat="detail in self.allDetails"
        ng-if="self.message && detail.identifier && detail.viewerComponent && self.getDetailContent(detail.identifier)"
        message-detail-viewer-element="{{detail.viewerComponent}}"
        detail="detail"
        content="self.getDetailContent(detail.identifier)">
      </div>
      <div class="msg__matchScore" ng-if="self.message && self.matchScore">MatchScore: {{self.matchScorePercentage }}%</div>
      <div class="msg__text markdown" ng-if="self.message && !self.isConnectMessage() && !self.isOpenMessage() && !self.message.get('isParsable')">{{ self.noParsableContentPlaceholder }}</div>
      <div class="msg__text hide-in-responsive clickable" ng-if="!self.message">«Message not (yet) loaded. Click to Load»</div>
      <div class="msg__text show-in-responsive clickable" ng-if="!self.message">«Message not (yet) loaded. Tap to Load»</div>
    `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);

      this.noParsableContentPlaceholder =
        "«This message couldn't be displayed as it didn't contain," +
        "any parsable content! " +
        'Click on the "Show raw RDF data"-button in ' +
        'the footer of the page to see the "raw" message-data.»';

      this.allDetails = useCaseUtils.getAllDetails();

      const selectFromState = state => {
        const ownedAtom =
          this.connectionUri &&
          getOwnedAtomByConnectionUri(state, this.connectionUri);
        const connection =
          ownedAtom && ownedAtom.getIn(["connections", this.connectionUri]);
        const message =
          connection &&
          this.messageUri &&
          getIn(connection, ["messages", this.messageUri]);

        const content = get(message, "content");
        const matchScore = get(content, "matchScore");
        const text = get(content, "text");

        return {
          connection,
          message,
          messageType: message && message.get("messageType"),
          matchScorePercentage: matchScore && matchScore * 100,
          matchScore,
          text,
          content,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri", "self.messageUri"],
        this
      );
    }

    isConnectMessage() {
      return this.messageType === won.WONMSG.connectMessage;
    }

    isOpenMessage() {
      return this.messageType === won.WONMSG.openMessage;
    }

    getDetail(key) {
      const detail = this.allDetails && this.allDetails[key];
      if (!detail) {
        console.error(
          "Could not find detail with key: ",
          key,
          " in:  ",
          this.allDetails
        );
      }
      return detail;
    }

    getDetailContent(key) {
      return key && this.content && this.content.get(key);
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      messageUri: "=",
      connectionUri: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.messageContent", ["hc.marked"])
  .directive("messageDetailViewerElement", [
    "$compile",
    function($compile) {
      return {
        restrict: "A",
        scope: {
          content: "=",
          detail: "=",
        },
        link: function(scope, element, attrs) {
          const customTag = attrs.messageDetailViewerElement;
          if (!customTag) return;

          const customElem = angular.element(
            `<${customTag} class="won-in-message" detail="detail" content="content"></${customTag}>`
          );

          element.append($compile(customElem)(scope));
        },
      };
    },
  ])
  .directive("wonMessageContent", genComponentConf).name;
