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
export const petriNetWorkflow = {
  identifier: "petriNetWorkflow",
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
        "wf:hasInlinePetriNetDefinition": value.data,
      };

      return { "won:hasPetriNet": workflow };
    }
    return { "won:hasPetriNet": undefined };
  },
  parseFromRDF: function(jsonLDImm) {
    const wflw = jsonLDImm && jsonLDImm.get("won:hasPetriNet");

    let workflow = {
      processUri: get(wflw, "@id"),
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

export const petriNetTransition = {
  identifier: "petriNetTransition",
  label: "Transition",
  icon: "#ico36_detail_workflow", //TODO: CORRECT ICON
  component: "won-petrinettransition-picker",
  viewerComponent: "won-petrinettransition-viewer",
  messageEnabled: true,
  parseToRDF: function({ value }) {
    if (value && value.petriNetUri && value.transitionUri) {
      return {
        "@id": value.petriNetUri,
        "wf:firesTransition": {
          "@id": value.transitionUri,
        },
      };
    } else {
      //we need to return this predicate object part so we do not run into troubles on creating needs/messages
      return undefined;
    }
  },
  parseFromRDF: function(jsonLDImm, rawMessageJsonLDImm) {
    //ParseFromRDF currently only works with one found transitionUri petriNetUri Triple
    if (rawMessageJsonLDImm) {
      const crawlRawMessageContentGraphs = function(rawMessageJsonLDImm) {
        const rawMessageJsonLdContentGraphs =
          rawMessageJsonLDImm && rawMessageJsonLDImm.get("@graph");

        if (
          rawMessageJsonLdContentGraphs &&
          Immutable.List.isList(rawMessageJsonLdContentGraphs)
        ) {
          let result;

          rawMessageJsonLdContentGraphs.map(value => {
            const partialResult = crawlRawMessageContentGraphs(value);
            if (partialResult) {
              result = partialResult;
            }
          });
          return result;
        } else {
          const fireTransitionContent = rawMessageJsonLDImm.get(
            "wf:firesTransition"
          );

          if (fireTransitionContent) {
            const transitionUri = fireTransitionContent.get("@id");
            const petriNetUri = rawMessageJsonLDImm.get("@id");

            if (transitionUri && petriNetUri) {
              console.log(
                "reached graph-end and found firesTransition",
                rawMessageJsonLDImm.toJS(),
                rawMessageJsonLDImm
              );

              return {
                petriNetUri: petriNetUri,
                transitionUri: transitionUri,
              };
            }
          }
          return undefined;
        }
      };

      const results = crawlRawMessageContentGraphs(rawMessageJsonLDImm);

      return results && Immutable.fromJS(results);
    }
    return undefined;
  },
  generateHumanReadable: function({ value, includeLabel }) {
    if (value && value.petriNetUri && value.transitionUri) {
      const humanReadableString =
        " fire: <" +
        value.transitionUri +
        "> in PetriNetUri: <" +
        value.petriNetUri +
        ">";

      return includeLabel
        ? this.label + ": " + humanReadableString
        : humanReadableString;
    }
    return undefined;
  },
};
