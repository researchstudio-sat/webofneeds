import { generateIdString, get } from "../../app/utils.js";
import Immutable from "immutable";

export const bpmnWorkflow = {
  identifier: "bpmnWorkflow",
  label: "BPMN",
  icon: "#ico36_detail_workflow",
  placeholder: "",
  //accepts: "application/octet-stream",
  accepts: "",
  component: "won-workflow-picker",
  viewerComponent: "won-workflow-viewer",
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    if (value && value.name && value.data) {
      //do not check for value.type might not be present on some systems
      let workflow = {
        "@id":
          contentUri && identifier
            ? contentUri + "/" + identifier + "/" + generateIdString(10)
            : undefined,
        "@type": "s:FileObject",
        "s:name": value.name,
        "s:type": value.type,
        "s:data": value.data,
      };

      return { "won:hasBpmnWorkflow": workflow };
    }
    return { "won:hasBpmnWorkflow": undefined };
  },
  parseFromRDF: function(jsonLDImm) {
    const wflw = jsonLDImm && jsonLDImm.get("won:hasBpmnWorkflow");

    let workflow = {
      name: get(wflw, "s:name"),
      type: get(wflw, "s:type"),
      data: get(wflw, "s:data"),
    };
    if (workflow.name && workflow.data) {
      //do not check for value.type might not be present on some systems
      return Immutable.fromJS(workflow);
    }

    return undefined;
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value && value.name) {
      return includeLabel ? this.label + ": " + value.name : value.name;
    }
    return undefined;
  },
};
export const petrinetWorkflow = {
  identifier: "petrinetWorkflow",
  label: "Petrinet",
  icon: "#ico36_detail_workflow",
  placeholder: "",
  //accepts: "application/octet-stream",
  accepts: "",
  component: "won-petrinet-picker",
  viewerComponent: "won-petrinet-viewer",
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    if (value && value.name && value.data) {
      //do not check for value.type might not be present on some systems
      let workflow = {
        "@id":
          contentUri && identifier
            ? contentUri + "/" + identifier + "/" + generateIdString(10)
            : undefined,
        "@type": "s:FileObject",
        "s:name": value.name,
        "s:type": value.type,
        "proc:hasInlinePetriNetDefinition": value.data,
      };

      return { "won:hasPetrinet": workflow };
    }
    return { "won:hasPetrinet": undefined };
  },
  parseFromRDF: function(jsonLDImm) {
    const wflw = jsonLDImm && jsonLDImm.get("won:hasPetrinet");

    let workflow = {
      name: get(wflw, "s:name"),
      type: get(wflw, "s:type"),
      data: get(wflw, "wf:hasInlinePetriNetDefinition"),
    };
    if (workflow.name && workflow.data) {
      //do not check for value.type might not be present on some systems
      return Immutable.fromJS(workflow);
    }

    return undefined;
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value && value.name) {
      return includeLabel ? this.label + ": " + value.name : value.name;
    }
    return undefined;
  },
};
/*<https://192.168.124.49:8443/won/resource/event/xlifb1yz7opl/petrinetWorkflow/fzkwlb9xdp> wf:firesTransition <http://purl.org/webofneeds/process/taxi#DriverArrivedAtPickupLocation> .
 <this:eventuri> won:hasTextMessage "Dear passenger, I'm waiting at the pickup location. You have 5 minutes." .*/
export const fireTransition = {
  identifier: "fireTransition",
  label: "Transition",
  icon: "#ico36_detail_workflow", //TODO: CORRECT ICON
  placeholder: "",
  //accepts: "application/octet-stream",
  accepts: "",
  component: "won-firetransition-picker",
  viewerComponent: "won-firetransition-viewer",
  messageEnabled: true,
  parseToRDF: function({ value, identifier, contentUri }) {
    //TODO: CORRECT PARSETORDF
    if (value && value.name && value.data) {
      //do not check for value.type might not be present on some systems
      let workflow = {
        "@id":
          contentUri && identifier
            ? contentUri + "/" + identifier + "/" + generateIdString(10)
            : undefined,
        "@type": "s:FileObject",
        "s:name": value.name,
        "s:type": value.type,
        "proc:hasInlinePetriNetDefinition": value.data,
      };

      return { "won:hasPetrinet": workflow };
    }
    return { "won:hasPetrinet": undefined };
  },
  parseFromRDF: function(jsonLDImm) {
    //TODO: CORRECT PARSEFROMRDF
    const wflw = jsonLDImm && jsonLDImm.get("won:hasPetrinet");

    let workflow = {
      name: get(wflw, "s:name"),
      type: get(wflw, "s:type"),
      data: get(wflw, "wf:hasInlinePetriNetDefinition"),
    };
    if (workflow.name && workflow.data) {
      //do not check for value.type might not be present on some systems
      return Immutable.fromJS(workflow);
    }

    return undefined;
  },
  generateHumanReadable: function({ value, includeLabel }) {
    //TODO: GENERATE HUMANREADABL
    if (value && value.name) {
      return includeLabel ? this.label + ": " + value.name : value.name;
    }
    return undefined;
  },
};
