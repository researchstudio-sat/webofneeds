/**
 * Defines a set of details that will only be visible within a specific 'implementation'
 * you will need to alter the identifier, label, icon, parseToRDF, and parseFromRDF if
 * you want to use it.
 */
import WonRangeViewer from "../../app/components/details/viewer/range-viewer.jsx";
import WonNumberViewer from "../../app/components/details/viewer/number-viewer.jsx";
import WonSelectViewer from "../../app/components/details/viewer/select-viewer.jsx";
import WonDropdownViewer from "../../app/components/details/viewer/dropdown-viewer.jsx";

import WonRangePicker from "../../app/components/details/picker/range-picker.jsx";
import WonNumberPicker from "../../app/components/details/picker/number-picker.jsx";
import WonSelectPicker from "../../app/components/details/picker/select-picker.jsx";
import WonDropdownPicker from "../../app/components/details/picker/dropdown-picker.jsx";

export const range = {
  identifier: function() {
    throw "abstract Detail does not override necessary identifier";
  },
  label: function() {
    throw "abstract Detail does not override necessary label";
  },
  minLabel: function() {
    throw "abstract Detail does not override necessary minLabel";
  },
  maxLabel: function() {
    throw "abstract Detail does not override necessary maxLabel";
  },
  minPlaceholder: undefined,
  maxPlaceholder: undefined,
  icon: undefined,
  component: WonRangePicker,
  viewerComponent: WonRangeViewer,
  parseToRDF: function() {
    throw "abstract Detail does not override necessary function";
  },
  parseFromRDF: function() {
    throw "abstract Detail does not override necessary function";
  },
  generateHumanReadable: function() {
    throw "abstract Detail does not override necessary function";
  },
};
export const number = {
  identifier: function() {
    throw "abstract Detail does not override necessary identifier";
  },
  label: function() {
    throw "abstract Detail does not override necessary label";
  },
  icon: undefined,
  component: WonNumberPicker,
  viewerComponent: WonNumberViewer,
  parseToRDF: function() {
    throw "abstract Detail does not override necessary function";
  },
  parseFromRDF: function() {
    throw "abstract Detail does not override necessary function";
  },
  generateHumanReadable: function() {
    throw "abstract Detail does not override necessary function";
  },
};
export const select = {
  identifier: function() {
    throw "abstract Detail does not override necessary identifier";
  },
  label: function() {
    throw "abstract Detail does not override necessary label";
  },
  icon: undefined,
  component: WonSelectPicker,
  viewerComponent: WonSelectViewer,
  multiSelect: false,
  options: function() {
    throw 'abstract Detail does not override necessary options array(structure: [{value: val, label: "labeltext"}...]';
    /**
       * e.g. number of rooms ....
       [
        {value: "1", label: "one"},
        {value: "2", label: "two"},
        {value: "3", label: "three"},
        {value: "4", label: "four"},
        {value: "5+", label: "more"},
       ]
       */
  },
  parseToRDF: function() {
    throw "abstract Detail does not override necessary function";
  },
  parseFromRDF: function() {
    throw "abstract Detail does not override necessary function";
  },
  generateHumanReadable: function() {
    throw "abstract Detail does not override necessary function";
  },
};
export const dropdown = {
  identifier: function() {
    throw "abstract Detail does not override necessary identifier";
  },
  label: function() {
    throw "abstract Detail does not override necessary label";
  },
  icon: undefined,
  component: WonDropdownPicker,
  viewerComponent: WonDropdownViewer,
  options: function() {
    throw 'abstract Detail does not override necessary options array(structure: [{value: val, label: "labeltext"}...]';
    /**
       * e.g. relationship status....
        [
         {value: "single", label: "single"},
         {value: "married", label: "married"},
         {value: "complicated", label: "it's complicated"},
         {value: "divorced", label: "divorced"},
         {value: "free", label: "free for all"},
        ]
       */
  },
  parseToRDF: function() {
    throw "abstract Detail does not override necessary function";
  },
  parseFromRDF: function() {
    throw "abstract Detail does not override necessary function";
  },
  generateHumanReadable: function() {
    throw "abstract Detail does not override necessary function";
  },
};
