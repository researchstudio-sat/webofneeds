/**
 * Component for rendering atom-title, type and timestamp
 * Created by ksinger on 10.04.2017.
 */
import angular from "angular";
import won from "../won-es6.js";
import "ng-redux";
import squareImageModule from "./square-image.js";
import groupImageModule from "./group-image.js";
import { actionCreators } from "../actions/actions.js";
import { labels, relativeTime } from "../won-label-utils.js";
import { attach, getIn, get } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import * as atomUtils from "../atom-utils.js";
import { isChatToGroup } from "../connection-utils.js";
import { getHumanReadableStringFromMessage } from "../reducers/atom-reducer/parse-message.js";
import {
  selectLastUpdateTime,
  getOwnedAtomByConnectionUri,
  getAtoms,
} from "../selectors/general-selectors.js";
import { getUnreadMessagesByConnectionUri } from "../selectors/message-selectors.js";
import { getMessagesByConnectionUri } from "../selectors/message-selectors.js";
import connectionStateModule from "./connection-state.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";

import "~/style/_connection-header.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
      <div class="ch__icon" ng-if="!self.connectionOrAtomsLoading && !self.isConnectionToGroup">
          <won-square-image
            class="ch__icon__theiratom"
            uri="::self.targetAtom.get('uri')">
          </won-square-image>
      </div>
      <won-group-image
        class="ch__groupicons"
        ng-if="!self.connectionOrAtomsLoading && self.isConnectionToGroup"
        connection-uri="self.connectionUri">
      </won-group-image>
      <div class="ch__right" ng-if="!self.connectionOrAtomsLoading">
        <div class="ch__right__topline" ng-if="!self.targetAtomFailedToLoad">
          <div class="ch__right__topline__title" ng-if="!self.isDirectResponseFromRemote && self.targetAtom.get('humanReadable')" title="{{ self.targetAtom.get('humanReadable') }}">
            {{ self.targetAtom.get('humanReadable') }}
          </div>
          <div class="ch__right__topline__notitle" ng-if="!self.isDirectResponseFromRemote && !self.targetAtom.get('humanReadable')" title="no title">
            no title
          </div>
          <div class="ch__right__topline__notitle" ng-if="self.isDirectResponseFromRemote" title="Direct Response">
            Direct Response
          </div>
        </div>
        <div class="ch__right__subtitle" ng-if="!self.targetAtomFailedToLoad">
          <span class="ch__right__subtitle__type">
            <span class="ch__right__subtitle__type__persona"
              ng-if="self.remotePersonaName && !self.isGroupChatEnabled">
              {{self.remotePersonaName}}
            </span>
            <span class="ch__right__subtitle__type__groupchat"
              ng-if="self.isGroupChatEnabled && !self.isChatEnabled">
              Group Chat
            </span>
            <span class="ch__right__subtitle__type__groupchat"
              ng-if="self.isGroupChatEnabled && self.isChatEnabled">
              Group Chat enabled
            </span>
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
        <div class="ch__right__topline" ng-if="self.targetAtomFailedToLoad">
          <div class="ch__right__topline__notitle">
            Remote Atom Loading failed
          </div>
        </div>
        <div class="ch__right__subtitle" ng-if="self.targetAtomFailedToLoad">
          <span class="ch__right__subtitle__type">
            <span class="ch__right__subtitle__type__state">
              Atom might have been deleted, you might want to close this connection.
            </span>
          </span>
        </div>
      </div>
      <div class="ch__icon" ng-if="self.connectionOrAtomsLoading">
          <div class="ch__icon__skeleton"></div>
      </div>
      <div class="ch__right" ng-if="self.connectionOrAtomsLoading">
        <div class="ch__right__topline">
          <div class="ch__right__topline__title"></div>
          <div class="ch__right__topline__date"></div>
        </div>
        <div class="ch__right__subtitle">
          <span class="ch__right__subtitle__type"></span>
        </div>
      </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.labels = labels;
      this.WON = won.WON;
      const selectFromState = state => {
        const ownedAtom = getOwnedAtomByConnectionUri(
          state,
          this.connectionUri
        );
        const connection =
          ownedAtom && ownedAtom.getIn(["connections", this.connectionUri]);
        const targetAtom =
          connection && get(getAtoms(state), connection.get("targetAtomUri"));
        const allMessages = getMessagesByConnectionUri(
          state,
          this.connectionUri
        );
        const unreadMessages = getUnreadMessagesByConnectionUri(
          state,
          this.connectionUri
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

        const groupMembers = targetAtom && targetAtom.get("groupMembers");

        const remotePersonaUri = get(targetAtom, "heldBy");
        const remotePersona =
          remotePersonaUri && getIn(state, ["atoms", remotePersonaUri]);
        const remotePersonaName = get(remotePersona, "humanReadable");

        return {
          connection,
          groupMembersArray: groupMembers && groupMembers.toArray(),
          groupMembersSize: groupMembers ? groupMembers.size : 0,
          ownedAtom,
          targetAtom,
          remotePersonaName,
          isConnectionToGroup: isChatToGroup(
            state.get("atoms"),
            get(ownedAtom, "uri"),
            this.connectionUri
          ),
          isDirectResponseFromRemote: atomUtils.isDirectResponseAtom(
            targetAtom
          ),
          isGroupChatEnabled: atomUtils.hasGroupSocket(targetAtom),
          isChatEnabled: atomUtils.hasChatSocket(targetAtom),
          latestMessageHumanReadableString,
          latestMessageUnread,
          unreadMessageCount:
            unreadMessages && unreadMessages.size > 0
              ? unreadMessages.size
              : undefined,
          friendlyTimestamp:
            targetAtom &&
            relativeTime(
              selectLastUpdateTime(state),
              this.timestamp || targetAtom.get("lastUpdateDate")
            ),
          targetAtomFailedToLoad:
            targetAtom &&
            getIn(state, [
              "process",
              "atoms",
              targetAtom.get("uri"),
              "failedToLoad",
            ]),
          connectionOrAtomsLoading:
            !connection ||
            !targetAtom ||
            !ownedAtom ||
            getIn(state, [
              "process",
              "atoms",
              ownedAtom.get("uri"),
              "loading",
            ]) ||
            getIn(state, [
              "process",
              "atoms",
              targetAtom.get("uri"),
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

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.connectionUri"],
        this
      );

      classOnComponentRoot(
        "won-is-loading",
        () => this.connectionOrAtomsLoading,
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
      connectionUri: "=",
      /**
       * one of:
       * - "fullpage" (NOT_YET_IMPLEMENTED) (used in post-info page)
       * - "medium" (NOT_YET_IMPLEMENTED) (used in incoming/outgoing requests)
       * - "small" (NOT_YET_IMPLEMENTED) (in matches-list)
       */
      //size: '=',
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.connectionHeader", [
    squareImageModule,
    groupImageModule,
    connectionStateModule,
  ])
  .directive("wonConnectionHeader", genComponentConf).name;
