/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import { get } from "../utils.js";

//import { useDispatch } from "react-redux";

export default function WonConnectionAgreementDetails({ connection }) {
  ///const dispatch = useDispatch();

  const agreementData = get(connection, "agreementData");

  /**
   * agreementData: {
        agreementUris: Immutable.Set(),
        pendingProposalUris: Immutable.Set(),
        pendingCancellationProposalUris: Immutable.Set(),
        cancellationPendingAgreementUris: Immutable.Set(),
        acceptedCancellationProposalUris: Immutable.Set(),
        cancelledAgreementUris: Immutable.Set(),
        rejectedMessageUris: Immutable.Set(),
        retractedMessageUris: Immutable.Set(),
        proposedMessageUris: Immutable.Set(),
        claimedMessageUris: Immutable.Set(),
      },
   */
  const agreementUris = get(agreementData, "agreementUris");
  const proposalUris = get(agreementData, "pendingProposalUris");
  const claimUris = get(agreementData, "claimedMessageUris");
  const proposeToCancelUris = get(
    agreementData,
    "cancellationPendingAgreementUris"
  );

  const agreementElementa =
    agreementUris &&
    agreementUris.map((agreementUri, index) => {
      return <div key={agreementUri + index}>Agreement: {agreementUri}</div>;
    });
  const proposalElments =
    proposalUris &&
    proposalUris.map((proposalUri, index) => {
      return <div key={proposalUri + index}>Proposal: {proposalUris}</div>;
    });
  const claimElements =
    claimUris &&
    claimUris.map((claimUri, index) => {
      return <div key={claimUri + index}>Claim: {claimUri}</div>;
    });

  const proposeToCancelElements =
    proposeToCancelUris &&
    proposeToCancelUris.map((proposeToCancelUri, index) => {
      return (
        <div key={proposeToCancelUri + index}>
          ProposeToCancel: {proposeToCancelUri}
        </div>
      );
    });

  return (
    <won-connection-agreement-details>
      <div>
        TEST
        {agreementElementa}
        {proposalElments}
        {claimElements}
        {proposeToCancelElements}
      </div>
    </won-connection-agreement-details>
  );
}
WonConnectionAgreementDetails.propTypes = {
  connection: PropTypes.object.isRequired,
};
