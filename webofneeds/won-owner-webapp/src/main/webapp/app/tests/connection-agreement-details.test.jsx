import * as fs from "fs";
import { readRdfStream } from "../service/rdf-utils";
import vocab from "../service/vocab.js";
import * as N3 from "n3";

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
  buddy: vocab.BUDDY.baseUri,
  rdf: "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
  agr: vocab.AGR.baseUri,
  pay: vocab.PAYMENT.baseUri,
  gr: "http://purl.org/goodrelations/v1#",
  wf: vocab.WORKFLOW.baseUri,
  rdfg: "http://www.w3.org/2004/03/trix/rdfg-1/",
};

// eslint-disable-next-line no-undef
describe("TEST ConnectionAgreementDetails", () => {
  /**
   * TEST 1: Read TriG document with one agreement, parse and write it
   */
  // eslint-disable-next-line no-undef
  it("TEST 1: one-agreement", async () => {
    const parser = new N3.Parser({ format: "application/trig" });
    const writer = new N3.Writer({
      format: "application/trig",
      //prefixes: minimalContext,
    });

    //let endResult = "1";
    const rdfStream = fs.createReadStream(
      "./app/tests/resources/one-agreement.trig"
    );
    let inputData = await readRdfStream(rdfStream);
    console.log("Input Data: ", inputData);

    const parsedData = parser.parse(inputData.toString());
    console.log("Parsed Data: ", parsedData);
    writer.addQuads(parsedData);

    const endResult = await new Promise((resolve, reject) => {
      writer.end((error, result) => {
        resolve(result);
      });
      writer.error(error => {
        reject(error);
      });
    });
    console.log("End Result: ", endResult);
    //expect(endResult).toStrictEqual(inputData);
  });
  /**
   * TEST 2: Read TriG document with two agreements
   */
  // eslint-disable-next-line no-undef
  it("TEST 2: two-agreement", done => {
    const parser = new N3.Parser({ format: "application/trig" });
    const writer = new N3.Writer({
      format: "application/trig",
      prefixes: minimalContext,
    });
    const rdfStream = fs.createReadStream(
      "./app/tests/resources/two-agreements.trig"
    );

    rdfStream.on("data", function(data) {
      console.log("Incoming Data: ", data.toString());
      const parsedData = parser.parse(data.toString());
      console.log("Parsed Data: ", parsedData);
      writer.addQuads(parsedData);
      writer.end((error, result) => {
        console.log("Written parsed Data: ", result);
        done();
      });
    });
  });

  /**
   * TEST 3: Read TriG document with two agreements in one graph
   */
  // eslint-disable-next-line no-undef
  it("TEST 2: two-agreement", done => {
    const parser = new N3.Parser({ format: "application/trig" });
    const writer = new N3.Writer({
      format: "application/trig",
      prefixes: minimalContext,
    });
    const rdfStream = fs.createReadStream(
      "./app/tests/resources/two-same-graph-agreements.trig"
    );

    rdfStream.on("data", function(data) {
      console.log("Incoming Data: ", data.toString());
      const parsedData = parser.parse(data.toString());
      console.log("Parsed Data: ", parsedData);
      writer.addQuads(parsedData);
      writer.end((error, result) => {
        console.log("Written parsed Data: ", result);
        done();
      });
    });
  });
});
