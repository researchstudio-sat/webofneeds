/**
 * Component for rendering atom-title
 * Created by ksinger on 10.04.2017.
 */
import angular from "angular";
import inviewModule from "angular-inview";
import "ng-redux";
import atomMapModule from "./atom-map.js";
import { actionCreators } from "../actions/actions.js";
import { relativeTime } from "../won-label-utils.js";
import { attach, getIn, get } from "../utils.js";
import { connect2Redux } from "../won-utils.js";
import { selectLastUpdateTime } from "../selectors/general-selectors.js";
import { classOnComponentRoot } from "../cstm-ng-utils.js";
import * as atomUtils from "../atom-utils.js";
import * as processUtils from "../process-utils.js";

import "~/style/_atom-card.scss";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
    <!-- Icon Information -->
    <div class="card__icon clickable"
        ng-if="self.atomLoaded"
        style="background-color: {{self.showDefaultIcon && self.iconBackground}}"
        ng-class="{
          'won-is-persona': self.isPersona,
          'inactive': self.isInactive,
          'card__icon--map': self.showMap,
        }"
        ng-click="self.router__stateGo('post', {postUri: self.atom.get('uri')})">
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
            ng-if="self.atomImage"
            alt="{{self.atomImage.get('name')}}"
            ng-src="data:{{self.atomImage.get('type')}};base64,{{self.atomImage.get('data')}}"/>
        <won-atom-map class="location" locations="[self.atomLocation]" ng-if="self.showMap" disable-controls current-location="self.currentLocation">
        </won-atom-map>
    </div>
    <div class="card__icon__skeleton" ng-if="!self.atomLoaded"
      in-view="$inview && self.atomToLoad && self.ensureAtomIsLoaded()">
    </div>
    <!-- Main Information -->
    <div class="card__main clickable"
        ng-if="self.atomLoaded" ng-click="self.router__stateGo('post', {postUri: self.atom.get('uri')})"
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
                    {{ self.atomTypeLabel }}
                </span>
            </span>
            <div class="card__main__subtitle__date">
                {{ self.friendlyTimestamp }}
            </div>
        </div>
    </div>
    <div class="card__main" ng-if="self.atomFailedToLoad">
        <div class="card__main__topline">
            <div class="card__main__topline__notitle">
                Atom Loading failed
            </div>
        </div>
        <div class="card__main__subtitle">
            <span class="card__main__subtitle__type">
                Atom might have been deleted.
            </span>
        </div>
    </div>
    <div class="card__main" ng-if="self.atomLoading || self.atomToLoad">
        <div class="card__main__topline">
            <div class="card__main__topline__title"></div>
        </div>
        <div class="card__main__subtitle">
            <span class="card__main__subtitle__type"></span>
        </div>
    </div>
    <!-- Attached Persona Info -->
    <div class="card__persona clickable" ng-if="self.atomLoaded && self.persona && self.atomHasHoldableSocket" ng-click="self.router__stateGoCurrent({viewAtomUri: self.personaUri})">
          <img class="card__persona__icon"
              ng-if="::self.personaIdenticonSvg"
              alt="Auto-generated title image for persona that holds the atom"
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
    <div class="card__nopersona" ng-if="(self.atomLoaded && !self.persona && self.atomHasHoldableSocket) || !self.atomLoaded">
        <span class="card__nopersona__label" ng-if="self.atomLoaded">No Persona attached</span>
    </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.nc4dbg = this;

      const selectFromState = state => {
        const atom = getIn(state, ["atoms", this.atomUri]);
        const isDirectResponse = atomUtils.isDirectResponseAtom(atom);
        const responseToUri =
          isDirectResponse && getIn(atom, ["content", "responseToUri"]);
        const responseToAtom =
          responseToUri && getIn(state, ["atoms", responseToUri]);

        const isPersona = atomUtils.isPersona(atom);

        const personaUri = get(atom, "heldBy");
        const persona = personaUri && getIn(state, ["atoms", personaUri]);
        const personaName = get(persona, "humanReadable");
        const personaHolds = persona && get(persona, "holds");
        const personaVerified =
          personaHolds && personaHolds.includes(this.atomUri);
        const process = get(state, "process");

        const personaIdenticonSvg = atomUtils.getIdenticonSvg(persona);
        const useCaseIcon = !isPersona
          ? atomUtils.getMatchedUseCaseIcon(atom)
          : undefined;
        const iconBackground = !isPersona
          ? atomUtils.getBackground(atom)
          : undefined;
        const identiconSvg = !useCaseIcon
          ? atomUtils.getIdenticonSvg(atom)
          : undefined;

        const atomImage = atomUtils.getDefaultImage(atom);
        const atomLocation = atomUtils.getLocation(atom); //include the comment instead of the false, to display location in the atom-card

        return {
          //General
          responseToAtom,
          atom,
          atomTypeLabel: atom && atomUtils.generateTypeLabel(atom),
          personaUri,
          persona,
          personaName,
          personaVerified,
          atomHasHolderSocket: atomUtils.hasHolderSocket(atom),
          atomHasHoldableSocket: atomUtils.hasHoldableSocket(atom),
          atomLoaded: processUtils.isAtomLoaded(process, this.atomUri),
          atomLoading: processUtils.isAtomLoading(process, this.atomUri),
          atomToLoad: processUtils.isAtomToLoad(process, this.atomUri),
          atomFailedToLoad: processUtils.hasAtomFailedToLoad(
            process,
            this.atomUri
          ),
          isPersona,
          isInactive: atomUtils.isInactive(atom),
          isDirectResponse: isDirectResponse,
          isGroupChatEnabled: atomUtils.hasGroupSocket(atom),
          isChatEnabled: atomUtils.hasChatSocket(atom),
          friendlyTimestamp:
            atom &&
            relativeTime(
              selectLastUpdateTime(state),
              get(atom, "lastUpdateDate")
            ),
          //image specific
          iconBackground,
          useCaseIcon,
          identiconSvg,
          personaIdenticonSvg,
          atomImage,
          atomLocation,
          showDefaultIcon: !atomImage && !atomLocation, //if no image and no location are present we display the defaultIcon in the card__icon area, instead of next to the title
          showMap: !atomImage && atomLocation, //if no image is present but a location is, we display a map instead
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.atomUri", "self.currentLocation"],
        this
      );

      classOnComponentRoot("won-is-loading", () => this.atomLoading, this);
      classOnComponentRoot("won-is-toload", () => this.atomToLoad, this);
    }

    ensureAtomIsLoaded() {
      if (
        this.atomUri &&
        !this.atomLoaded &&
        !this.atomLoading &&
        this.atomToLoad
      ) {
        this.atoms__fetchUnloadedAtom(this.atomUri);
      }
    }

    hasTitle() {
      if (this.isDirectResponse && this.responseToAtom) {
        return !!this.responseToAtom.get("humanReadable");
      } else {
        return !!this.atom.get("humanReadable");
      }
    }

    generateTitle() {
      if (this.isDirectResponse && this.responseToAtom) {
        return "Re: " + this.responseToAtom.get("humanReadable");
      } else {
        return this.atom.get("humanReadable");
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
      currentLocation: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.atomCard", [inviewModule.name, atomMapModule])
  .directive("wonAtomCard", genComponentConf).name;
