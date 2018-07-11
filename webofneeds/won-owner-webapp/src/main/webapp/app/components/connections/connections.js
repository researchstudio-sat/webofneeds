import angular from "angular";
import won from "../../won-es6.js";
import Immutable from "immutable";
import sendRequestModule from "../send-request.js";
import postMessagesModule from "../post-messages.js";
import postInfoModule from "../post-info.js";
import connectionsOverviewModule from "../connections-overview.js";
import createPostModule from "../create-post.js";
import createSearchModule from "../create-search.js";
import usecasePickerModule from "../usecase-picker.js";
import usecasePickerContentModule from "../usecase-picker-content.js";
import { attach, getIn, callBuffer } from "../../utils.js";
import { actionCreators } from "../../actions/actions.js";
import {
  selectNeedByConnectionUri,
  selectAllOwnNeeds,
} from "../../selectors.js";
import { resetParams } from "../../configRouting.js";

const serviceDependencies = ["$element", "$ngRedux", "$scope"];

class ConnectionsController {
  constructor() {
    attach(this, serviceDependencies, arguments);
    const self = this;
    this.WON = won.WON;
    this.resetParams = resetParams;
    this.open = {};

    this.SEARCH = "search";
    this.POST = "post";

    const scrollArea = this.$element[0].querySelector(".connectionscontent");

    this.scrollBuffer = callBuffer(scrollPosition => {
      self.mainViewScrolled(scrollPosition);
    }, 100);

    scrollArea.addEventListener("scroll", () => {
      self.scrollBuffer(scrollArea.scrollTop);
    });

    const selectFromState = state => {
      const selectedPostUri = decodeURIComponent(
        getIn(state, ["router", "currentParams", "postUri"])
      );
      const selectedPost =
        selectedPostUri && state.getIn(["needs", selectedPostUri]);
      const showUseCases = getIn(state, [
        "router",
        "currentParams",
        "showUseCases",
      ]);

      const useCase = getIn(state, ["router", "currentParams", "useCase"]);

      const connectionUri = decodeURIComponent(
        getIn(state, ["router", "currentParams", "connectionUri"])
      );
      const need =
        connectionUri && selectNeedByConnectionUri(state, connectionUri);
      const connection = need && need.getIn(["connections", connectionUri]);
      const connectionType =
        need &&
        connectionUri &&
        need.getIn(["connections", connectionUri, "state"]);

      const ownNeeds = selectAllOwnNeeds(state).filter(
        post =>
          !(
            (post.get("isWhatsAround") || post.get("isWhatsNew")) &&
            post.get("state") === won.WON.InactiveCompacted
          )
      );

      let connections = Immutable.Map();

      ownNeeds &&
        ownNeeds.map(function(need) {
          connections = connections.merge(need.get("connections"));
        });

      const theme = getIn(state, ["config", "theme", "name"]);

      return {
        theme,
        welcomeTemplate:
          "./skin/" +
          theme +
          "/" +
          getIn(state, ["config", "theme", "welcomeTemplate"]),
        WON: won.WON,
        selectedPost,
        connection,
        connectionType,
        useCase,
        showUseCases,
        hasConnections: connections && connections.size > 0,
        hasOwnNeeds: ownNeeds && ownNeeds.size > 0,
        open,
        mainViewScroll: state.get("mainViewScroll"),
        showWelcomePage: !(ownNeeds && ownNeeds.size > 0),
      };
    };

    const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(
      this
    );
    this.$scope.$on("$destroy", disconnect);

    this.$scope.$watch("self.mainViewScroll", newValue => {
      if (newValue !== undefined)
        requestAnimationFrame(() => (scrollArea.scrollTop = newValue));
    });
  }

  selectedNeed(needUri) {
    this.router__stateGoCurrent({
      connectionUri: undefined,
      postUri: needUri,
      showUseCases: undefined,
      useCase: undefined,
    }); //TODO: Maybe leave the connectionUri in the parameters to go back when closing a selected need
  }

  selectedConnection(connectionUri) {
    this.markAsRead(connectionUri);
    this.router__stateGoCurrent({
      connectionUri,
      postUri: undefined,
      showUseCases: undefined,
      useCase: undefined,
    });
  }

  /*
  isMobileDevice() {
    console.log("Screen height", screen.height);
    let ua = navigator.userAgent.toLowerCase();
    //let isAndroid = ua.indexOf("android") > -1; //&& ua.indexOf("mobile");
    let isMobile = ua.indexOf("mobile") > -1;
    if (isMobile) {
      console.log("MobileDevice");
      return true;
    }
    return false;
  }*/

  markAsRead(connectionUri) {
    const need = selectNeedByConnectionUri(
      this.$ngRedux.getState(),
      connectionUri
    );
    const connections = need && need.get("connections");
    const connection = connections && connections.get(connectionUri);

    if (
      connection &&
      connection.get("unread") &&
      connection.get("state") !== won.WON.Connected
    ) {
      const payload = {
        connectionUri: connectionUri,
        needUri: need.get("uri"),
      };

      this.connections__markAsRead(payload);
    }
  }
}

ConnectionsController.$inject = [];

export default angular
  .module("won.owner.components.connections", [
    sendRequestModule,
    postMessagesModule,
    postInfoModule,
    usecasePickerModule,
    usecasePickerContentModule,
    createPostModule,
    createSearchModule,
    connectionsOverviewModule,
  ])
  .controller("ConnectionsController", [
    ...serviceDependencies,
    ConnectionsController,
  ]).name;
