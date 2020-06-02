/**
 * Created by ms on 27.05.2020.
 */
import React from "react";
import PropTypes from "prop-types";
import { get } from "../utils.js";
import * as N3 from "n3";

export default function WonConnectionAgreementDetails({ connection }) {
  const agreementDataset = get(connection, "agreementDataset");

  let agreementParsedQuads = [];
  agreementDataset.forEach(quad => {
    const agreementWriter = new N3.Writer({
      format: "application/trig",
      prefixes: {
        con: "https://w3id.org/won/content#",
      },
    });
    agreementWriter.addQuad(quad);
    agreementWriter.end((error, result) => {
      console.log(result);
      agreementParsedQuads.push(result);
    });
  });

  const agreementQuadsElement =
    agreementParsedQuads &&
    agreementParsedQuads.map((quad, index) => {
      return (
        <div key={quad + index}>
          {quad}
          <hr />
        </div>
      );
    });

  return (
    <won-connection-agreement-details>
      <div>
        <div className="pm__content__agreement__title">Agreements</div>
        {agreementQuadsElement}
      </div>
    </won-connection-agreement-details>
  );
}
WonConnectionAgreementDetails.propTypes = {
  connection: PropTypes.object.isRequired,
};
