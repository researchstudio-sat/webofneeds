/**
 * Component for rendering need-title
 * Created by ksinger on 10.04.2017.
 */
import angular from "angular";
import inviewModule from "angular-inview";
import "ng-redux";
import needMapModule from "./need-map.js";
import { actionCreators } from "../actions/actions.js";
import { relativeTime } from "../won-label-utils.js";
import { attach, getIn, get } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { selectLastUpdateTime } from "../selectors/general-selectors.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";
import * as needUtils from "../need-utils.js";
import * as processUtils from "../process-utils.js";

import "style/_need-card.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
    <div class="card__icon clickable"
        ng-if="self.needLoaded"
        style="background-color: {{self.showDefaultIcon && self.iconBackground}}"
        ng-class="{
          'won-is-persona': self.isPersona,
          'inactive': self.isInactive,
        }"
        ng-click="self.router__stateGo('post', {postUri: self.need.get('uri')})">
        <div class="identicon usecaseimage"
            ng-if="self.showDefaultIcon && self.useCaseIcon">
            <svg>
                <use xlink:href="{{ ::self.useCaseIcon }}" href="{{ ::self.useCaseIcon }}"></use>
            </svg>
        </div>
        <img class="identicon"
            ng-if="self.showDefaultIcon && self.identiconSvg"
            alt="Auto-generated title image"
            ng-src="data:image/svg+xml;base64,{{::self.identiconSvg}}"/>
        <img class="image"
            ng-if="self.needImage"
            alt="{{self.needImage.get('name')}}"
            ng-src="data:{{self.needImage.get('type')}};base64,{{self.needImage.get('data')}}"/>
        <!--won-need-map class="location" locations="[self.needLocation]" ng-if="!self.needImage && self.needLocation">
        </won-need-map-->
    </div>
    <div class="card__icon__skeleton" ng-if="!self.needLoaded"
      in-view="$inview && self.needToLoad && self.ensureNeedIsLoaded()">
    </div>
    <div class="card__main clickable"
        ng-if="self.needLoaded" ng-click="self.router__stateGo('post', {postUri: self.need.get('uri')})"
        ng-class="{
          'card__main--showIcon': !self.showDefaultIcon,
        }">
        <div class="card__main__icon" ng-if="!self.showDefaultIcon" style=" background-color: {{!self.hasImage && self.iconBackground}}">
            <div class="card__main__icon__usecaseimage"
                ng-if="self.useCaseIcon">
                <svg>
                    <use xlink:href="{{ ::self.useCaseIcon }}" href="{{ ::self.useCaseIcon }}"></use>
                </svg>
            </div>
            <img class="card__main__icon__identicon"
                ng-if="self.identiconSvg"
                alt="Auto-generated title image"
                ng-src="data:image/svg+xml;base64,{{::self.identiconSvg}}"/>
        </div>
        <div class="card__main__topline">
            <div class="card__main__topline__title" ng-if="self.hasTitle()">
                {{ self.generateTitle() }}
            </div>
            <div class="card__main__topline__notitle" ng-if="!self.hasTitle() && self.isDirectResponse">
                RE: no title
            </div>
            <div class="card__main__topline__notitle" ng-if="!self.hasTitle() && !self.isDirectResponse">
                no title
            </div>
        </div>
        <div class="card__main__subtitle">
            <span class="card__main__subtitle__type">
                <span class="card__main__subtitle__type__groupchat"
                    ng-if="self.isGroupChatEnabled && !self.isChatEnabled">
                    Group Chat
                </span>
                <span class="card__main__subtitle__type__groupchat"
                    ng-if="self.isGroupChatEnabled && self.isChatEnabled">
                    Group Chat enabled
                </span>
                <span>
                    {{ self.needTypeLabel }}
                </span>
            </span>
            <div class="card__main__subtitle__date">
                {{ self.friendlyTimestamp }}
            </div>
        </div>
        <div class="card__main__location" ng-if="self.needLocation">
          <svg class="card__main__location__icon">
              <use xlink:href="#ico36_detail_location" href="#ico36_detail_location"></use>
          </svg>
          <span class="card__main__location__address">
              {{ self.needLocation.get('address') }}
          </span>
        </div>
    </div>
    <div class="card__persona clickable" ng-if="self.needLoaded && self.persona" ng-click="self.router__stateGoCurrent({viewNeedUri: self.personaUri})">
          <img class="card__persona__icon"
              ng-if="::self.personaIdenticonSvg"
              alt="Auto-generated title image for persona that holds the need"
              ng-src="data:image/svg+xml;base64,{{::self.personaIdenticonSvg}}"/>
          <div class="card__persona__name"
              ng-if="self.personaName">
              <span class="card__persona__name__label">{{ self.personaName }}</span>
              <span class="card__persona__name__verification card__persona__name__verification--verified" ng-if="self.personaVerified" title="The Persona-Relation of this Post is verified by the Persona">Verified</span>
              <span class="card__persona__name__verification card__persona__name__verification--unverified" ng-if="!self.personaVerified" title="The Persona-Relation of this Post is NOT verified by the Persona">Unverified!</span>
          </div>
          <div class="card__persona__websitelabel" ng-if="self.personaWebsite">Website:</div>
          <a class="card__persona__websitelink" target="_blank" href="{{self.personaWebsite}}" ng-if="self.personaWebsite">{{ self.personaWebsite }}</a>
    </div>
    <div class="card__main" ng-if="self.needFailedToLoad">
        <div class="card__main__topline">
            <div class="card__main__topline__notitle">
                Need Loading failed
            </div>
        </div>
        <div class="card__main__subtitle">
            <span class="card__main__subtitle__type">
                Need might have been deleted.
            </span>
        </div>
    </div>
    <div class="card__main" ng-if="self.needLoading || self.needToLoad">
        <div class="card__main__topline">
            <div class="card__main__topline__title"></div>
        </div>
        <div class="card__main__subtitle">
            <span class="card__main__subtitle__type"></span>
        </div>
    </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.nc4dbg = this;

      const selectFromState = state => {
        const need = getIn(state, ["needs", this.needUri]);
        const isDirectResponse = needUtils.isDirectResponseNeed(need);
        const responseToUri =
          isDirectResponse && getIn(need, ["content", "responseToUri"]);
        const responseToNeed =
          responseToUri && getIn(state, ["needs", responseToUri]);

        const isPersona = needUtils.isPersona(need);

        const personaUri = get(need, "heldBy");
        const persona = personaUri && getIn(state, ["needs", personaUri]);
        const personaName = get(persona, "humanReadable");
        const personaHolds = persona && get(persona, "holds");
        const personaVerified =
          personaHolds && personaHolds.includes(this.needUri);
        const process = get(state, "process");

        const personaIdenticonSvg = needUtils.getIdenticonSvg(persona);
        const useCaseIcon = !isPersona
          ? needUtils.getMatchedUseCaseIcon(need)
          : undefined;
        const iconBackground = !isPersona
          ? needUtils.getBackground(need)
          : undefined;
        const identiconSvg = !useCaseIcon
          ? needUtils.getIdenticonSvg(need)
          : undefined;

        const needImage = needUtils.getDefaultImage(need);
        const needLocation = needUtils.getLocation(need); //include the comment instead of the false, to display location in the need-card

        return {
          //General
          responseToNeed,
          need,
          needTypeLabel: need && needUtils.generateNeedTypeLabel(need),
          personaUri,
          persona,
          personaName,
          personaVerified,
          needLoaded: processUtils.isNeedLoaded(process, this.needUri),
          needLoading: processUtils.isNeedLoading(process, this.needUri),
          needToLoad: processUtils.isNeedToLoad(process, this.needUri),
          needFailedToLoad: processUtils.hasNeedFailedToLoad(
            process,
            this.needUri
          ),
          isPersona,
          isInactive: needUtils.isInactive(need),
          isDirectResponse: isDirectResponse,
          isGroupChatEnabled: needUtils.hasGroupFacet(need),
          isChatEnabled: needUtils.hasChatFacet(need),
          friendlyTimestamp:
            need &&
            relativeTime(
              selectLastUpdateTime(state),
              get(need, "lastUpdateDate")
            ),
          //image specific
          iconBackground,
          useCaseIcon,
          identiconSvg,
          personaIdenticonSvg,
          needImage,
          needLocation,
          showDefaultIcon: !needImage /*&& !needLocation*/, //if no image and no location are present we display the defaultIcon in the card__icon area, instead of next to the title
        };
      };

      connect2Redux(selectFromState, actionCreators, ["self.needUri"], this);

      classOnComponentRoot("won-is-loading", () => this.needLoading, this);
      classOnComponentRoot("won-is-toload", () => this.needToLoad, this);
      classOnComponentRoot("won-is-invisible", () => this.hideNeed(), this);
    }

    ensureNeedIsLoaded() {
      if (
        this.needUri &&
        !this.needLoaded &&
        !this.needLoading &&
        this.needToLoad
      ) {
        this.needs__fetchUnloadedNeed(this.needUri);
      }
    }

    //FIXME: THIS and the corresponding css-class need to be removed, this is solely to prevent loaded/but inactive need to show up for now
    hideNeed() {
      return this.needLoaded && needUtils.isInactive(this.need);
    }

    hasTitle() {
      if (this.isDirectResponse && this.responseToNeed) {
        return !!this.responseToNeed.get("humanReadable");
      } else {
        return !!this.need.get("humanReadable");
      }
    }

    generateTitle() {
      if (this.isDirectResponse && this.responseToNeed) {
        return "Re: " + this.responseToNeed.get("humanReadable");
      } else {
        return this.need.get("humanReadable");
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
      needUri: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.needCard", [inviewModule.name, needMapModule])
  .directive("wonNeedCard", genComponentConf).name;