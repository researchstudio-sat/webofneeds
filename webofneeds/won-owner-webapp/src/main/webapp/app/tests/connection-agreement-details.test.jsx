import * as Graphy from "graphy";
import { parseRDFJSQuadToFactoryQuad } from "../service/rdf-utils";
const factory = Graphy["core.data.factory"];

/**
 * TEST 1: FactoryQuad Test
 */
// eslint-disable-next-line no-undef
describe("TEST 1: FactoryQuad Test", () => {
  const value = "wm:/W1kFB7BiaoqdKAfybFebcMbBHuecnMbBYueERHUvvz8L41";
  const content = "https://w3id.org/won/content#text";
  const namedNodeResource = { value, termType: "NamedNode" };
  const objectResource = { value, termType: "Literal" };
  const instertQuad = {
    subject: namedNodeResource,
    predicate: { value: content, termType: "NamedNode" },
    object: objectResource,
    graph: namedNodeResource,
  };

  const expectedQuad = factory.quad(
    factory.namedNode(value),
    factory.namedNode(content),
    factory.literal(value),
    factory.namedNode(value)
  );

  // eslint-disable-next-line no-undef
  it("instert Quad", () => {
    // eslint-disable-next-line no-undef
    expect(parseRDFJSQuadToFactoryQuad(instertQuad)).toStrictEqual(
      expectedQuad
    );
  });
});

/**
 * TEST 2: Quad String Test
 *
 * Outcome:
 * <wm:/W1pm7C7p3iUbSATDFdSVFrVSMDTnWnX7sti1LKE6acbsye> {
 *  <wm:/W1nk43sqZ7VsH2t7uJkcqctXpH1p2g5SRA4GXvF5j9Zs54>
 *         <https://w3id.org/won/content#text>
 *                  "JUnit Proposal 1" .
 * }
 *
 */
// eslint-disable-next-line no-undef
describe("TEST 2: Quad String Test", () => {
  const namedNode = "NamedNode";
  const literal = "Literal";
  const instertQuad = {
    subject: {
      termType: namedNode,
      value: "wm:/W1nk43sqZ7VsH2t7uJkcqctXpH1p2g5SRA4GXvF5j9Zs54",
    },
    predicate: {
      termType: namedNode,
      value: "https://w3id.org/won/content#text",
    },
    object: { termType: literal, value: "JUnit Proposal 1" },
    graph: {
      termType: namedNode,
      value: "wm:/W1pm7C7p3iUbSATDFdSVFrVSMDTnWnX7sti1LKE6acbsye",
    },
  };
  const expectedQuad =
    '<wm:/W1pm7C7p3iUbSATDFdSVFrVSMDTnWnX7sti1LKE6acbsye> { <wm:/W1nk43sqZ7VsH2t7uJkcqctXpH1p2g5SRA4GXvF5j9Zs54> <https://w3id.org/won/content#text> "JUnit Proposal 1" . }';

  // eslint-disable-next-line no-undef
  it("instert Quad", () => {
    // eslint-disable-next-line no-undef
    expect(parseRDFJSQuadToFactoryQuad(instertQuad).terse()).toStrictEqual(
      expectedQuad
    );
  });
});
