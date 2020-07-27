/**
 * Created by ms on 27.05.2020.
 */
import React, { useState, useEffect, useRef } from "react";
import * as N3 from "n3";
import { usePrevious } from "../cstm-react-utils.js";
import PropTypes from "prop-types";
import won from "../service/won";
import * as connectionUtils from "../redux/utils/connection-utils.js";

import "../../style/_connection-agreement-details.scss";

export default function WonConnectionAgreementDetails({ connection }) {
  const lastAgreementDataset = connectionUtils.getAgreementDataset(connection);
  const previousAgreementDataset =
    lastAgreementDataset &&
    usePrevious(lastAgreementDataset, useRef, useEffect);
  const [parsedData, setParsedData] = useState("");

  const writer = new N3.Writer({
    format: "application/trig",
    prefixes: won.minimalContext,
  });

  if (
    lastAgreementDataset &&
    lastAgreementDataset != previousAgreementDataset
  ) {
    writer.addQuads(lastAgreementDataset);
    writer.end((error, result) => {
      if (parsedData !== result) {
        setParsedData(result);
      }
    });
  }

  return (
    <won-connection-agreement-details>
      <div className="pm__content__agreement__title">Agreements</div>
      <div className="cad__quad">{parsedData}</div>
    </won-connection-agreement-details>
  );
}

WonConnectionAgreementDetails.propTypes = {
  connection: PropTypes.object.isRequired,
};
