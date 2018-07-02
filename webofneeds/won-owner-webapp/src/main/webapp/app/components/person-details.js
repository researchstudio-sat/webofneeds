import angular from "angular";
import { attach, delay, getIn } from "../utils.js";
import { DomCache } from "../cstm-ng-utils.js";

const serviceDependencies = ["$ngRedux", "$scope", "$element"];
function genComponentConf() {
  let template = `
        <p class="post-info__details">{{self.title}}</p>
        <p class="post-info__details">{{self.name}}</p>
        <p class="post-info__details" ng-if="self.company && self.position">
          {{self.position}} at {{self.company}}
        </p>
        <p class="post-info__details" ng-if="!self.company && self.position">
          {{self.position}}
        </p>
        <p class="post-info__details" ng-if="self.company && !self.position">
          {{self.company}}
        </p>
        <p class="post-info__details" ng-if="self.skills">Skills: {{self.skills}}</p>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      this.domCache = new DomCache(this.$element);

      //TODO: debug; deleteme
      window.person4dbg = this;

      this.title = undefined;
      this.name = undefined;
      this.company = undefined;
      this.position = undefined;
      this.skills = undefined;

      delay(0).then(() => this.parsePersonDetails(this.person));
    }

    parsePersonDetails(person) {
      if (person) {
        this.title = getIn(person, ["title"]);
        this.name = getIn(person, ["name"]);
        this.company = getIn(person, ["company", "s:name"]);
        this.position = getIn(person, ["position"]);
        this.skills = getIn(person, ["skills"])
          ? getIn(person, ["skills"])
              .toJS()
              .toString()
          : "";
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
      person: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.personDetailsModule", [])
  .directive("wonPersonDetails", genComponentConf).name;
