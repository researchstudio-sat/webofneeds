import angular from "angular";
import "ng-redux";
import atomMapModule from "../atom-map.js";
import atomSuggestionsIndicatorModule from "../atom-suggestions-indicator.js";
import { actionCreators } from "../../actions/actions.js";
import { relativeTime } from "../../won-label-utils.js";
import { getIn, get } from "../../utils.js";
import { attach } from "../../cstm-ng-utils.js";
import { connect2Redux } from "../../configRedux.js";
import { selectLastUpdateTime } from "../../redux/selectors/general-selectors.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";

import "~/style/_other-card.scss";
import Immutable from "immutable";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `    
        <!-- Icon Information -->
        <div class="card__icon clickable"
          style="background-color: {{self.showDefaultIcon && self.iconBackground}}"
          ng-class="{
            'inactive': self.isInactive,
            'card__icon--map': self.showMap,
          }"
          ng-click="::self.atomClick(self.atomUri)">
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
      <!-- Main Information -->
      <div class="card__main clickable" ng-click="::self.atomClick(self.atomUri)"
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
      <!-- Attached Persona Info -->
      <div class="card__persona clickable" ng-if="::self.showPersona && self.persona && self.atomHasHoldableSocket" ng-click="self.router__stateGo('post', {postUri: self.personaUri})">
            <img class="card__persona__icon"
                ng-if="self.showPersonaIdenticon"
                alt="Auto-generated title image for persona that holds the atom"
                ng-src="data:image/svg+xml;base64,{{::self.personaIdenticonSvg}}"/>
            <img class="card__persona__icon"
                ng-if="self.showPersonaImage"
                alt="{{self.personaImage.get('name')}}"
                ng-src="data:{{self.personaImage.get('type')}};base64,{{self.personaImage.get('data')}}"/>
            <div class="card__persona__name"
                ng-if="self.personaName">
                <span class="card__persona__name__label">{{ self.personaName }}</span>
                <span class="card__persona__name__verification card__persona__name__verification--verified" ng-if="self.personaVerified" title="The Persona-Relation of this Post is verified by the Persona">Verified</span>
                <span class="card__persona__name__verification card__persona__name__verification--unverified" ng-if="!self.personaVerified" title="The Persona-Relation of this Post is NOT verified by the Persona">Unverified!</span>
            </div>
            <div class="card__persona__websitelabel" ng-if="self.personaWebsite">Website:</div>
            <a class="card__persona__websitelink" target="_blank" href="{{self.personaWebsite}}" ng-if="self.personaWebsite">{{ self.personaWebsite }}</a>
      </div>
      <div class="card__nopersona" ng-if="::self.showPersona && !self.persona && self.atomHasHoldableSocket">
          <span class="card__nopersona__label">No Persona attached</span>
      </div>
      <won-atom-suggestions-indicator
          ng-if="::self.showSuggestions"
          class="card__indicators"
          atom-uri="::self.atomUri"
          on-selected="::self.showAtomSuggestions(self.atomUri)">
      </won-atom-suggestions-indicator>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const atom = getIn(state, ["atoms", this.atomUri]);
        const useCaseIcon = atomUtils.getMatchedUseCaseIcon(atom);
        const iconBackground = atomUtils.getBackground(atom);
        const identiconSvg = !useCaseIcon
          ? atomUtils.getIdenticonSvg(atom)
          : undefined;

        const isDirectResponse = atomUtils.isDirectResponseAtom(atom);
        const responseToUri =
          isDirectResponse && getIn(atom, ["content", "responseToUri"]);
        const responseToAtom = getIn(state, ["atoms", responseToUri]);

        const atomImage = atomUtils.getDefaultImage(atom);
        const atomLocation = atomUtils.getLocation(atom); //include the comment instead of the false, to display location in the atom-card
        const personaUri = atomUtils.getHeldByUri(atom);
        const persona = getIn(state, ["atoms", personaUri]);
        const personaName = get(persona, "humanReadable");
        const personaHolds = persona && get(persona, "holds");
        const personaVerified =
          personaHolds && personaHolds.includes(this.atomUri);
        const personaIdenticonSvg = atomUtils.getIdenticonSvg(persona);
        const personaImage = atomUtils.getDefaultPersonaImage(persona);

        return {
          isDirectResponse: isDirectResponse,
          isInactive: atomUtils.isInactive(atom),
          responseToAtom,
          atom,
          persona,
          personaName,
          personaVerified,
          personaUri,
          atomTypeLabel: atomUtils.generateTypeLabel(atom),
          atomHasHoldableSocket: atomUtils.hasHoldableSocket(atom),
          isGroupChatEnabled: atomUtils.hasGroupSocket(atom),
          isChatEnabled: atomUtils.hasChatSocket(atom),
          friendlyTimestamp:
            atom &&
            relativeTime(
              selectLastUpdateTime(state),
              get(atom, "lastUpdateDate")
            ),
          showPersonaImage: personaImage,
          showPersonaIdenticon: !personaImage && personaIdenticonSvg,
          personaIdenticonSvg,
          personaImage,
          showMap: !atomImage && atomLocation, //if no image is present but a location is, we display a map instead
          atomLocation,
          atomImage,
          showDefaultIcon: !atomImage && !atomLocation, //if no image and no location are present we display the defaultIcon in the card__icon area, instead of next to the title
          useCaseIcon,
          iconBackground,
          identiconSvg,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        [
          "self.atomUri",
          "self.currentLocation",
          "self.showSuggestions",
          "self.showPersona",
          "self.disableDefaultAtomInteraction",
        ],
        this
      );
    }

    hasTitle() {
      if (this.isDirectResponse && this.responseToAtom) {
        return !!this.responseToAtom.get("humanReadable");
      } else {
        return !!this.atom && !!this.atom.get("humanReadable");
      }
    }

    generateTitle() {
      if (this.isDirectResponse && this.responseToAtom) {
        return "Re: " + this.responseToAtom.get("humanReadable");
      } else {
        return this.atom && this.atom.get("humanReadable");
      }
    }

    showAtomSuggestions(atomUri) {
      this.showAtomTab(atomUri, "SUGGESTIONS");
    }

    atomClick(atomUri) {
      if (!this.disableDefaultAtomInteraction) {
        this.showAtomTab(atomUri, "DETAIL");
      }
    }

    showAtomTab(atomUri, tab = "DETAIL") {
      this.atoms__selectTab(
        Immutable.fromJS({ atomUri: atomUri, selectTab: tab })
      );
      this.router__stateGo("post", { postUri: atomUri });
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
      showSuggestions: "=",
      showPersona: "=",
      disableDefaultAtomInteraction: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.otherCard", [
    atomMapModule,
    atomSuggestionsIndicatorModule,
  ])
  .directive("wonOtherCard", genComponentConf).name;
