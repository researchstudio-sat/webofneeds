/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import vocab from "~/app/service/vocab";
import * as atomUtils from "~/app/redux/utils/atom-utils";

import "~/style/_atom-feed.scss";
import * as connectionUtils from "~/app/redux/utils/connection-utils";

export default function WonAtomFeed({ atom }) {
  const connections = atomUtils
    .getConnections(atom, vocab.WON.ClosedCompacted, true)
    .toOrderedMap()
    .sortBy(conn => {
      const lastUpdateDate = connectionUtils.getLastUpdateDate(conn);
      return lastUpdateDate && lastUpdateDate.getTime();
    })
    .reverse();

  return <won-atom-feed>TODO: FEED {connections.size}</won-atom-feed>;
}
WonAtomFeed.propTypes = {
  atom: PropTypes.object.isRequired,
};
