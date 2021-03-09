/**
 * Created by ms on 15.02.2021
 */
import React from "react";
import { useSelector } from "react-redux";
import PropTypes from "prop-types";
import { get } from "~/app/utils";
import * as processSelectors from "~/app/redux/selectors/process-selectors";

import "~/style/_atom-content-requests.scss";

export default function WonAtomContentRequests({ atomUri }) {
  const atomRequests = useSelector(processSelectors.getAtomRequests(atomUri));
  const connectionContainerRequests = useSelector(
    processSelectors.getConnectionContainerRequests(atomUri)
  );

  const atomRequestElements = [];
  const connectionContainerRequestElements = [];

  const generateResponseCodeClasses = responseCode => {
    const cssClassNames = ["acrequests__item__code"];

    if ((responseCode >= 200 && responseCode < 300) || responseCode === 304) {
      cssClassNames.push("acrequests__item__code--success");
    } else {
      cssClassNames.push("acrequests__item__code--failure");
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
      </div>
      <div className="acrequests__cc">
        <div className="acrequests__atom__header">
          ConnectionContainer Request Status
        </div>
        {connectionContainerRequestElements.length > 0 ? (
          connectionContainerRequestElements
        ) : (
          <div className="acrequests__cc__nodata">
            No Atom Request Data available
          </div>
        )}
      </div>
    </won-atom-content-requests>
  );
}
WonAtomContentRequests.propTypes = {
  atomUri: PropTypes.object.isRequired,
};
