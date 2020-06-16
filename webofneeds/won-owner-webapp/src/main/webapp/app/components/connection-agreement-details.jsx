/**
 * Created by ms on 27.05.2020.
 */
import React, { useState, useEffect, useRef } from "react";
import { usePrevious } from "../cstm-react-utils.js";
import PropTypes from "prop-types";
import won from "../service/won";
import { get } from "../utils.js";
import { parseRDFJSQuadToFactoryQuad } from "../service/rdf-utils";
import * as Graphy from "graphy";

import "../../style/_connection-agreement-details.scss";

export default function WonConnectionAgreementDetails({ connection }) {
  const lastAgreementDataset = get(connection, "agreementDataset");
  const previousAgreementDataset =
    lastAgreementDataset &&
    usePrevious(lastAgreementDataset, useRef, useEffect);
  const [graphyParsedData, setGraphyParsedData] = useState([]);
  const [graphyElement, setGraphyElement] = useState("");

  // Graphy solution

  const trigScribe = Graphy["content.trig.scribe"];
  let dsScriber = trigScribe({
    prefixes: {
      ...won.minimalContext,
    },
  });

  dsScriber.on("data", sTrig => {
    if (!graphyParsedData.includes(sTrig + "")) {
      graphyParsedData.push(sTrig + "");
      setGraphyParsedData(graphyParsedData);
      generateGraphyElement(graphyParsedData);
      console.log(sTrig + "");
    }
  });

  function generateGraphyElement(dataArray) {
    let parsedElement = "";
    dataArray.forEach(element => (parsedElement += element));
    setGraphyElement(parsedElement);
  }

  if (
    lastAgreementDataset &&
    lastAgreementDataset !== previousAgreementDataset
  ) {
    lastAgreementDataset.forEach(quad => {
      dsScriber.write(parseRDFJSQuadToFactoryQuad(quad));
    });
    dsScriber.end();
  }

  return (
    <won-connection-agreement-details>
      <div className="pm__content__agreement__title">Agreements</div>
      <div className="cad__quad">{graphyElement}</div>
    </won-connection-agreement-details>
  );
}

WonConnectionAgreementDetails.propTypes = {
  connection: PropTypes.object.isRequired,
};
