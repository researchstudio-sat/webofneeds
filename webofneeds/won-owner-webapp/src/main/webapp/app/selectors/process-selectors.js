/**
 * Created by quasarchimaere on 21.01.2019.
 */
import { getIn } from "../utils.js";

export function isLoading(state) {
  //TODO: Incl. lookup to determine any other process being in loading
  return (
    getIn(state, ["process", "processingInitialLoad"]) ||
    getIn(state, ["process", "processingLogin"])
  );
}
