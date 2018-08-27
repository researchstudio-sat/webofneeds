import angular from "angular";

import won from "../../won-es6.js";
import { connect2Redux } from "../../won-utils.js";
import { getAllDetails } from "../../won-utils.js";
import { attach, getIn, get } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { selectNeedByConnectionUri } from "../../selectors.js";
// TODO: these should be replaced by importing defintions from config
import personViewerModule from "../details/viewer/person-viewer.js";
import descriptionViewerModule from "../details/viewer/description-viewer.js";
import locationViewerModule from "../details/viewer/location-viewer.js";
import tagsViewerModule from "../details/viewer/tags-viewer.js";
import travelActionViewerModule from "../details/viewer/travel-action-viewer.js";
import titleViewerModule from "../details/viewer/title-viewer.js";
import numberViewerModule from "../details/viewer/number-viewer.js";
import dateViewerModule from "../details/viewer/date-viewer.js";
import datetimeViewerModule from "../details/viewer/datetime-viewer.js";
import monthViewerModule from "../details/viewer/month-viewer.js";
import timeViewerModule from "../details/viewer/time-viewer.js";
import dropdownViewerModule from "../details/viewer/dropdown-viewer.js";
import selectViewerModule from "../details/viewer/select-viewer.js";
import rangeViewerModule from "../details/viewer/range-viewer.js";
import fileViewerModule from "../details/viewer/file-viewer.js";
import workflowViewerModule from "../details/viewer/workflow-viewer.js";
import petrinetViewerModule from "../details/viewer/petrinet-viewer.js";

import "style/_message-content.scss";

const serviceDependencies = ["$ngRedux", "$scope"];

function genComponentConf() {
  let template = `
      <div class="msg__text--prewrap" ng-if="self.message && self.text">{{ self.text }}</div> <!-- no spaces or newlines within the code-tag, because it is preformatted -->
      <div class="msg__content"
        ng-repeat="detail in self.allDetails"
        ng-if="self.message && detail.identifier && self.getDetailContent(detail.identifier)"
        message-detail-viewer-element="{{detail.viewerComponent}}"
        detail="detail"
        content="self.getDetailContent(detail.identifier)">
      </div>
      <div class="msg__matchScore" ng-if="self.message && self.matchScore">MatchScore: {{self.matchScorePercentage }}%</div>
      <div class="msg__text" ng-if="self.message && !self.isConnectMessage() && !self.isOpenMessage() && !self.message.get('isParsable')">{{ self.noParsableContentPlaceholder }}</div>
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
        'the main-menu on the right side of the navigationbar to see the "raw" message-data.»';

      this.allDetails = getAllDetails();

      const selectFromState = state => {
        const ownNeed =
          this.connectionUri &&
          selectNeedByConnectionUri(state, this.connectionUri);
        const connection =
          ownNeed && ownNeed.getIn(["connections", this.connectionUri]);
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
  .module("won.owner.components.messageContent", [
    personViewerModule,
    descriptionViewerModule,
    locationViewerModule,
    travelActionViewerModule,
    tagsViewerModule,
    titleViewerModule,
    numberViewerModule,
    dropdownViewerModule,
    dateViewerModule,
    timeViewerModule,
    datetimeViewerModule,
    monthViewerModule,
    selectViewerModule,
    rangeViewerModule,
    fileViewerModule,
    workflowViewerModule,
    petrinetViewerModule,
  ])
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
