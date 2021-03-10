/**
 * Created by ms on 15.02.2021
 */
import React from "react";
import { useSelector } from "react-redux";
import PropTypes from "prop-types";
import { get } from "~/app/utils";
import * as processSelectors from "~/app/redux/selectors/process-selectors";
import * as generalSelectors from "~/app/redux/selectors/general-selectors";
import * as processUtils from "~/app/redux/utils/process-utils";

import "~/style/_atom-content-requests.scss";

export default function WonAtomContentRequests({ atomUri }) {
  const atomRequests = useSelector(processSelectors.getAtomRequests(atomUri));
  const connectionContainerRequests = useSelector(
    processSelectors.getConnectionContainerRequests(atomUri)
  );

  const possibleRequestCredentials = useSelector(
    generalSelectors.getPossibleRequestCredentialsForAtom(atomUri)
  );

  const priorAtomRequests = useSelector(
    processSelectors.getAtomRequests(atomUri)
  );
  const priorConnectionContainerRequests = useSelector(
    processSelectors.getConnectionContainerRequests(atomUri)
  );

  const atomRequestElements = [];
  const connectionContainerRequestElements = [];
  const atomRequestCredentialsElements = [];
  const connectionContainerRequestCredentialsElements = [];

  const generateResponseCodeClasses = responseCode => {
    const cssClassNames = ["acrequests__item__code"];

    if ((responseCode >= 200 && responseCode < 300) || responseCode === 304) {
      cssClassNames.push("acrequests__item__code--success");
    } else {
      cssClassNames.push("acrequests__item__code--failure");
    }

    return cssClassNames.join(" ");
  };

  const generateCredentialsCodeClasses = (priorRequests, responseCode) => {
    const cssClassNames = ["acrequests__item"];

    if (
      processUtils.isUsedCredentialsSuccessfully(priorRequests, responseCode)
    ) {
      cssClassNames.push("acrequests__item--success");
    } else if (
      processUtils.isUsedCredentialsUnsuccessfully(priorRequests, responseCode)
    ) {
      cssClassNames.push("acrequests__item--failure");
    } else {
      cssClassNames.push("acrequests__item--unknown");
    }

    return cssClassNames.join(" ");
  };

  atomRequests &&
    atomRequests.map((request, idx) => {
      const responseCode = get(request, "code");
      atomRequestElements.push(
        <div key={"ar_" + idx} className="acrequests__item">
          <div className={generateResponseCodeClasses(responseCode)}>
            {responseCode}
          </div>
          <pre className="acrequests__item__content">
            {request && JSON.stringify(request.toJS(), undefined, 2)}
          </pre>
        </div>
      );
    });

  connectionContainerRequests &&
    connectionContainerRequests.map((request, idx) => {
      const responseCode = get(request, "code");
      connectionContainerRequestElements.push(
        <div key={"cr_" + idx} className="acrequests__item">
          <div className={generateResponseCodeClasses(responseCode)}>
            {responseCode}
          </div>
          <pre className="acrequests__item__content">
            {request && JSON.stringify(request.toJS(), undefined, 2)}
          </pre>
        </div>
      );
    });

  possibleRequestCredentials &&
    possibleRequestCredentials.map((credentials, idx) => {
      atomRequestCredentialsElements.push(
        <div
          key={"rc_" + idx}
          className={generateCredentialsCodeClasses(
            priorAtomRequests,
            credentials
          )}
        >
          <div className="acrequests__item__code">🔑</div>
          <pre className="acrequests__item__content">
            {credentials && JSON.stringify(credentials.toJS(), undefined, 2)}
          </pre>
        </div>
      );

      connectionContainerRequestCredentialsElements.push(
        <div
          key={"rc_" + idx}
          className={generateCredentialsCodeClasses(
            priorConnectionContainerRequests,
            credentials
          )}
        >
          <div className="acrequests__item__code">🔑</div>
          <pre className="acrequests__item__content">
            {credentials && JSON.stringify(credentials.toJS(), undefined, 2)}
          </pre>
        </div>
      );
    });

  return (
    <won-atom-content-requests>
      <div className="acrequests__atom">
        <div className="acrequests__atom__header">Atom Request Status</div>
        {atomRequestElements.length > 0 ? (
          atomRequestElements
        ) : (
          <div className="acrequests__atom__nodata">
            No Atom Request Data available
          </div>
        )}
        <div className="acrequests__atom__header">
          Possible Credentials to Use
        </div>
        {atomRequestCredentialsElements.length > 0 ? (
          atomRequestCredentialsElements
        ) : (
          <div className="acrequests__credentials__nodata">
            No Credentials found in state
          </div>
        )}
      </div>
      <div className="acrequests__cc">
        <div className="acrequests__cc__header">
          ConnectionContainer Request Status
        </div>
        {connectionContainerRequestElements.length > 0 ? (
          connectionContainerRequestElements
        ) : (
          <div className="acrequests__cc__nodata">
            No Atom Request Data available
          </div>
        )}
        <div className="acrequests__cc__header">
          Possible Credentials to Use
        </div>
        {connectionContainerRequestCredentialsElements.length > 0 ? (
          connectionContainerRequestCredentialsElements
        ) : (
          <div className="acrequests__credentials__nodata">
            No Credentials found in state
          </div>
        )}
      </div>
    </won-atom-content-requests>
  );
}
WonAtomContentRequests.propTypes = {
  atomUri: PropTypes.object.isRequired,
};
