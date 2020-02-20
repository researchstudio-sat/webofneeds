import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { get, getIn } from "../utils.js";
import WonAtomHeaderBig from "./atom-header-big.jsx";
import WonAtomMenu from "./atom-menu.jsx";
import WonAtomFooter from "./atom-footer.jsx";
import WonAtomContent from "./atom-content.jsx";
import * as generalSelectors from "../redux/selectors/general-selectors";
import * as atomUtils from "../redux/utils/atom-utils";
import * as viewUtils from "../redux/utils/view-utils";
import * as processSelectors from "../redux/selectors/process-selectors";

import "~/style/_atom-info.scss";

const mapStateToProps = (state, ownProps) => {
  const atom = getIn(state, ["atoms", ownProps.atomUri]);

  const isOwned = generalSelectors.isAtomOwned(state, ownProps.atomUri);

  const viewState = get(state, "view");
  const visibleTab = viewUtils.getVisibleTabByAtomUri(
    viewState,
    ownProps.atomUri
  );

  const atomLoading =
    !atom || processSelectors.isAtomLoading(state, ownProps.atomUri);

  const showFooter =
    !atomLoading &&
    visibleTab === "DETAIL" &&
    (atomUtils.isInactive(atom) ||
      (isOwned && atomUtils.hasEnabledUseCases(atom)) ||
      (!isOwned && atomUtils.hasReactionUseCases(atom)) ||
      (!isOwned &&
        (atomUtils.hasGroupSocket(atom) || atomUtils.hasChatSocket(atom))));

  return {
    className: ownProps.className,
    atomUri: ownProps.atomUri,
    defaultTab: ownProps.defaultTab,
    atomLoading,
    showFooter,
  };
};

class AtomInfo extends React.Component {
  render() {
    return (
      <won-atom-info
        class={
          (this.props.className ? this.props.className : "") +
          (this.props.atomLoading ? " won-is-loading " : "")
        }
      >
        <WonAtomHeaderBig atomUri={this.props.atomUri} />
        <WonAtomMenu
          atomUri={this.props.atomUri}
          defaultTab={this.props.defaultTab}
        />
        <WonAtomContent
          atomUri={this.props.atomUri}
          defaultTab={this.props.defaultTab}
        />
        {this.props.showFooter ? (
          <WonAtomFooter atomUri={this.props.atomUri} />
        ) : (
          undefined
        )}
      </won-atom-info>
    );
  }
}

AtomInfo.propTypes = {
  atomUri: PropTypes.string,
  defaultTab: PropTypes.string,
  atomLoading: PropTypes.bool,
  showFooter: PropTypes.bool,
  className: PropTypes.string,
};

export default connect(mapStateToProps)(AtomInfo);
