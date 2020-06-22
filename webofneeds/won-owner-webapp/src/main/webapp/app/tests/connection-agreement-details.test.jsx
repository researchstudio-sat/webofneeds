import * as Graphy from "graphy";
import * as fs from "fs";
import { parseRDFJSQuadToFactoryQuad } from "../service/rdf-utils";
import vocab from "../service/vocab.js";
const factory = Graphy["core.data.factory"];
const trigRead = Graphy["content.trig.read"];
/**
 * TEST 1: FactoryQuad Test
 */
// eslint-disable-next-line no-undef
describe("TEST ConnectionAgreementDetails", () => {
  // eslint-disable-next-line no-undef
  it("TEST 1: FactoryQuad Test", () => {
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
    expect(parseRDFJSQuadToFactoryQuad(instertQuad)).toStrictEqual(
      expectedQuad
    );
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
  it("TEST 2: Compare Quad", () => {
    const namedNode = "NamedNode";
    const literal = "Literal";
    const insertQuad = {
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
    expect(parseRDFJSQuadToFactoryQuad(insertQuad).terse()).toStrictEqual(
      expectedQuad
    );
  });

  /**
   * TEST 3: one-agreement
   * Read one-agreement.trig as result
   */
  // eslint-disable-next-line no-undef
  it("TEST 3: one-agreement", done => {
    fs.createReadStream("./app/tests/resources/one-agreement.trig")
      .pipe(trigRead())
      .on("data", y_quad => {
        console.dir(y_quad.isolate());
      })
      .on("eof", () => {
        console.log("done!");
        done();
      });
  });

  /**
   * TEST 4: Write trig
   * Read one-agreement.trig as result
   */
  const minimalContext = {
    msg: vocab.WONMSG.baseUri,
    won: vocab.WON.baseUri,
    con: vocab.WONCON.baseUri,
    match: vocab.WONMATCH.baseUri,
    demo: vocab.DEMO.baseUri,
    "wx-bot": vocab.BOT.baseUri,
    "wx-schema": vocab.WXSCHEMA.baseUri,
    hold: vocab.HOLD.baseUri,
    chat: vocab.CHAT.baseUri,
    group: vocab.GROUP.baseUri,
    review: vocab.REVIEW.baseUri,
    buddy: vocab.BUDDY.baseUri,
    rdf: "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    agr: vocab.AGR.baseUri,
    pay: vocab.PAYMENT.baseUri,
    gr: "http://purl.org/goodrelations/v1#",
    wf: vocab.WORKFLOW.baseUri,
    rdfg: "http://www.w3.org/2004/03/trix/rdfg-1/",
  };
  // eslint-disable-next-line no-undef
  it("TEST 4: Write trig", done => {
    const trigWrite = Graphy["content.trig.write"];
    let trigWriter = trigWrite();
    const value = "wm:/W1kFB7BiaoqdKAfybFebcMbBHuecnMbBYueERHUvvz8L41";
    const content = "https://w3id.org/won/content#text";

    const expectedQuad = factory.quad(
      factory.namedNode(value),
      factory.namedNode(content),
      factory.literal("Hello world", "https://schema.org/Text"),
      factory.namedNode(value)
    );
    trigWriter
      .on("data", quadString => {
        console.log(quadString + "");
      })
      .on("finish", () => {
        console.log("done!");
        done();
      });
    trigWriter.write({
      type: "prefixes",
      value: {
        ...minimalContext,
      },
      tokens: {
        graph: true, // output `GRAPH` tokens in TriG format
      },
    });
    trigWriter.write({
      type: "array",
      value: [expectedQuad, expectedQuad],
    });
    //}
    trigWriter.end();
  });
});
