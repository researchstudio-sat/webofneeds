/**
 * Created by quasarchimaere on 08.01.2019.
 */
import angular from "angular";
import won from "../won-es6.js";
import "ng-redux";
import squareImageModule from "./square-image.js";
import { actionCreators } from "../actions/actions.js";
import { labels, relativeTime } from "../won-label-utils.js";
import { attach, getIn } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { isDirectResponseNeed } from "../need-utils.js";
import { getHumanReadableStringFromMessage } from "../reducers/need-reducer/parse-message.js";
import {
  selectLastUpdateTime,
  getNonOwnedNeeds,
} from "../selectors/general-selectors.js";
import { getUnreadMessagesByConnectionUri } from "../selectors/message-selectors.js";
import { getMessagesByConnectionUri } from "../selectors/message-selectors.js";
import connectionStateModule from "./connection-state.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

import "style/_group-administration-header.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="ch__icon">
      </div>
      <div class="ch__right">
        <div class="ch__right__topline">
          <div class="ch__right__topline__title">
            Group Chat Administration
          </div>
        </div>
        <div class="ch__right__subtitle">
          <span class="ch__right__subtitle__type">
          </span>
        </div>
      </div>
      <!--div class="ch__icon" ng-if="!self.connectionOrNeedsLoading">
          <won-square-image
            class="ch__icon__theirneed"
            src="self.theirNeed.get('TODO')"
            title="self.theirNeed.get('humanReadable')"
            uri="self.theirNeed.get('uri')"
            ng-show="!self.hideImage">
          </won-square-image>
      </div>
      <div class="ch__right" ng-if="!self.connectionOrNeedsLoading">
        <div class="ch__right__topline" ng-if="!self.theirNeedFailedToLoad">
          <div class="ch__right__topline__title" ng-if="!self.isDirectResponseFromRemote && self.theirNeed.get('humanReadable')" title="{{ self.theirNeed.get('humanReadable') }}">
            {{ self.theirNeed.get('humanReadable') }}
          </div>
          <div class="ch__right__topline__notitle" ng-if="!self.isDirectResponseFromRemote && !self.theirNeed.get('humanReadable')" title="no title">
            no title
          </div>
          <div class="ch__right__topline__notitle" ng-if="self.isDirectResponseFromRemote" title="Direct Response">
            Direct Response
          </div>
        </div>
        <div class="ch__right__subtitle" ng-if="!self.theirNeedFailedToLoad">
          <span class="ch__right__subtitle__type">
            <won-connection-state 
              connection-uri="self.connection.get('uri')">
            </won-connection-state>
            <span class="ch__right__subtitle__type__state" ng-if="!self.unreadMessageCount && !self.latestMessageHumanReadableString">
              {{ self.connection && self.getTextForConnectionState(self.connection.get('state')) }}
            </span>
            <span class="ch__right__subtitle__type__unreadcount" ng-if="self.unreadMessageCount > 1">
              {{ self.unreadMessageCount }} unread Messages
            </span>
            <span class="ch__right__subtitle__type__unreadcount" ng-if="self.unreadMessageCount == 1 && !self.latestMessageHumanReadableString">
              {{ self.unreadMessageCount }} unread Message
            </span>
            <span class="ch__right__subtitle__type__message" ng-if="!(self.unreadMessageCount > 1) && self.latestMessageHumanReadableString"
              ng-class="{'won-unread': self.latestMessageUnread}">
              {{ self.latestMessageHumanReadableString }}
            </span>
          </span>
          <div class="ch__right__subtitle__date">
            {{ self.friendlyTimestamp }}
          </div>
        </div>
        <div class="ch__right__topline" ng-if="self.theirNeedFailedToLoad">
          <div class="ch__right__topline__notitle">
            Remote Need Loading failed
          </div>
        </div>
        <div class="ch__right__subtitle" ng-if="self.theirNeedFailedToLoad">
          <span class="ch__right__subtitle__type">
            <span class="ch__right__subtitle__type__state">
              Need might have been deleted, you might want to close this connection.
            </span>
          </span>
        </div>
      </div>
      <div class="ch__icon" ng-if="self.connectionOrNeedsLoading">
          <div class="ch__icon__skeleton"></div>
      </div>
      <div class="ch__right" ng-if="self.connectionOrNeedsLoading">
        <div class="ch__right__topline">
          <div class="ch__right__topline__title"></div>
          <div class="ch__right__topline__date"></div>
        </div>
        <div class="ch__right__subtitle">
          <span class="ch__right__subtitle__type"></span>
        </div>
      </div-->
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.labels = labels;
      this.WON = won.WON;
      const selectFromState = state => {
        const groupChatNeed = getIn(state, ["needs", this.needUri]);
        const ownedNeed = undefined;
        const connection =
          ownedNeed && ownedNeed.getIn(["connections", "FIXME: Remove"]);
        const theirNeed =
          connection &&
          getNonOwnedNeeds(state).get(connection.get("remoteNeedUri"));
        const allMessages = getMessagesByConnectionUri(state, "FIXME: Remove");
        const unreadMessages = getUnreadMessagesByConnectionUri(
          state,
          "FIXME: Remove"
        );

        const sortedMessages = allMessages && allMessages.toArray();
        if (sortedMessages) {
          sortedMessages.sort(function(a, b) {
            const aDate = a.get("date");
            const bDate = b.get("date");

            const aTime = aDate && aDate.getTime();
            const bTime = bDate && bDate.getTime();

            return bTime - aTime;
          });
        }

        const latestMessage = sortedMessages && sortedMessages[0];
        const latestMessageHumanReadableString =
          latestMessage && getHumanReadableStringFromMessage(latestMessage);
        const latestMessageUnread =
          latestMessage && latestMessage.get("unread");

        return {
          groupChatNeed,
          connection,
          ownedNeed,
          theirNeed,
          isDirectResponseFromRemote: isDirectResponseNeed(theirNeed),
          latestMessageHumanReadableString,
          latestMessageUnread,
          unreadMessageCount:
            unreadMessages && unreadMessages.size > 0
              ? unreadMessages.size
              : undefined,
          friendlyTimestamp:
            theirNeed &&
            relativeTime(
              selectLastUpdateTime(state),
              theirNeed.get("lastUpdateDate")
            ),
          theirNeedFailedToLoad:
            theirNeed &&
            getIn(state, [
              "process",
              "needs",
              theirNeed.get("uri"),
              "failedToLoad",
            ]),
          connectionOrNeedsLoading:
            !connection ||
            !theirNeed ||
            !ownedNeed ||
            getIn(state, [
              "process",
              "needs",
              ownedNeed.get("uri"),
              "loading",
            ]) ||
            getIn(state, [
              "process",
              "needs",
              theirNeed.get("uri"),
              "loading",
            ]) ||
            getIn(state, [
              "process",
              "connections",
              connection.get("uri"),
              "loading",
            ]),
        };
      };

      connect2Redux(selectFromState, actionCreators, ["self.needUri"], this);

      classOnComponentRoot(
        "won-is-loading",
        () => false && this.connectionOrNeedsLoading, //TODO: FIX THE LOADING BEHAVIOUR PROBABLY THERE IS NO LOADING BEHAVIOUR FOR THAT
        this
      );
    }

    getTextForConnectionState(state) {
      let stateText = this.labels.connectionState[state];
      if (!stateText) {
        stateText = "unknown connection state";
      }
      return stateText;
    }
  }
  Controller.$inject = serviceDependencies;
  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      needUri: "=",

      /**
       * if set, the avatar will be hidden
       */
      hideImage: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.groupAdministrationHeader", [
    squareImageModule,
    connectionStateModule,
  ])
  .directive("wonGroupAdministrationHeader", genComponentConf).name;
