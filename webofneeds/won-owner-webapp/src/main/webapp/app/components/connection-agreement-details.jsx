/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { get } from "../utils.js";

export default function WonConnectionAgreementDetails({ connection }) {
  const agreementDataset = get(connection, "agreementDataset");

  const agreementQuads =
    agreementDataset &&
    agreementDataset.map((quad, index) => {
      return (
        <div key={quad.subject.value + index}>
          &lt;
          {quad.subject.value}
          &gt; &lt;
          {quad.predicate.value}
          &gt; &lt;
          {quad.object.value}
          &gt; &lt;
          {quad.graph.value}
          &gt;
          <hr />
        </div>
      );
    });

  return (
    <won-connection-agreement-details>
      <div>
        <div className="pm__content__agreement__title">Agreements</div>
        {agreementQuads}
      </div>
    </won-connection-agreement-details>
  );
}
WonConnectionAgreementDetails.propTypes = {
  connection: PropTypes.object.isRequired,
};
