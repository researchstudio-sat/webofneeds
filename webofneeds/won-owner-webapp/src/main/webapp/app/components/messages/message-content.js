import angular from "angular";

import won from "../../won-es6.js";
import { connect2Redux } from "../../won-utils.js";
import { getAllDetails } from "../../won-utils.js";
import { attach, getIn } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import { selectNeedByConnectionUri } from "../../selectors.js";
import { labels } from "../../won-label-utils.js";
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

import "style/_message-content.scss";

const serviceDependencies = ["$ngRedux", "$scope"];

function genComponentConf() {
  let template = `
      <div class="msgcontent__header" ng-if="self.message && !self.isConnectionMessage()">
        <div class="msgcontent__header__type" ng-if="!self.isOtherMessage()">{{ self.labels.messageType[self.messageType] }}</div>
        <div class="msgcontent__header__type" ng-if="self.isOtherMessage()">{{ self.messageType }}</div>
      </div>
      <div class="msgcontent__body" ng-if="self.message">
        <div class="msgcontent__body__text--prewrap" ng-if="self.hasText">{{ self.text }}</div> <!-- no spaces or newlines within the code-tag, because it is preformatted -->
        <div class="msgcontent__body__content"
          ng-repeat="detail in self.allDetails"
          ng-if="detail.identifier && self.getDetailContent(detail.identifier)"
          message-detail-viewer-element="{{detail.viewerComponent}}"
          detail="detail"
          content="self.getDetailContent(detail.identifier)">
        </div>
        <div class="msgcontent__body__matchScore" ng-if="self.hasMatchScore">MatchScore: {{self.matchScorePercentage }}%</div>
        <div class="msgcontent__body__text" ng-if="!self.isConnectMessage() && !self.isOpenMessage() && !self.message.get('isParsable')">{{ self.noParsableContentPlaceholder }}</div>
      </div>
      <div class="msgcontent__body clickable" ng-if="!self.message">
        <div class="msgcontent__body__text hide-in-responsive">«Message not (yet) loaded. Click to Load»</div>
        <div class="msgcontent__body__text show-in-responsive">«Message not (yet) loaded. Tap to Load»</div>
      </div>
    `;

  class Controller {
    constructor(/* arguments = dependency injections */) {
      attach(this, serviceDependencies, arguments);

      this.labels = labels;

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

        const matchScore = message && getIn(message, ["content", "matchScore"]);

        const text = message && getIn(message, ["content", "text"]);

        return {
          connection,
          message,
          messageType: message && message.get("messageType"),
          matchScorePercentage: matchScore && matchScore * 100,
          hasMatchScore: !!matchScore,
          hasText: !!text,
          text,
          details: message && message.get("content"),
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri", "self.messageUri"],
        this
      );
    }

    isConnectionMessage() {
      return this.messageType === won.WONMSG.connectionMessage;
    }

    isConnectMessage() {
      return this.messageType === won.WONMSG.connectMessage;
    }

    isOpenMessage() {
      return this.messageType === won.WONMSG.openMessage;
    }

    isHintMessage() {
      return this.messageType === won.WONMSG.hintMessage;
    }

    isHintFeedbackMessage() {
      return this.messageType === won.WONMSG.hintFeedbackMessage;
    }

    isOtherMessage() {
      return !(
        this.isHintMessage() ||
        this.isHintFeedbackMessage() ||
        this.isOpenMessage() ||
        this.isConnectMessage() ||
        this.isConnectionMessage()
      );
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
      return key && this.details && this.details.get(key);
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
            `<${customTag} detail="detail" content="content"></${customTag}>`
          );

          element.append($compile(customElem)(scope));
        },
      };
    },
  ])
  .directive("wonMessageContent", genComponentConf).name;
