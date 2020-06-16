/**
 * Created by ms on 16.06.2020
 */
import * as Graphy from "graphy";
const factory = Graphy["core.data.factory"];

export function parseRDFJSQuadToFactoryQuad(rdfjsQuad) {
  return factory.quad(
    generateFactoryElementFromQuad(rdfjsQuad.subject),
    generateFactoryElementFromQuad(rdfjsQuad.predicate),
    generateFactoryElementFromQuad(rdfjsQuad.object),
    generateFactoryElementFromQuad(rdfjsQuad.graph)
  );
}

export function generateFactoryElementFromQuad(element) {
  if (element && element.termType) {
    switch (element.termType) {
      case "NamedNode":
        return factory.namedNode(element.value);
      case "Literal":
        return factory.literal(
          element.value,
          element.datatype
            ? element.datatype.value
            : element.language
              ? element.language
              : undefined
        );
      case "BlankNode":
        return factory.blankNode(element.value);
      case "Variable":
        return factory.variable(element.value);
      case "DefaultGraph":
        return factory.defaultGraph(element.value);
      default:
        return undefined;
    }
  }
  return undefined;
}
