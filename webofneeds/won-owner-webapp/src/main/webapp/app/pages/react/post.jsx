import React from "react";
import PropTypes from "prop-types";
import { connect } from "react-redux";
import { actionCreators } from "../../actions/actions.js";
import * as generalSelectors from "../../redux/selectors/general-selectors.js";
import { get, getIn, getQueryParams } from "../../utils.js";
import * as accountUtils from "../../redux/utils/account-utils.js";
import * as viewSelectors from "../../redux/selectors/view-selectors.js";
import * as processUtils from "../../redux/utils/process-utils.js";
import WonModalDialog from "../../components/modal-dialog.jsx";
import WonAtomInfo from "../../components/atom-info.jsx";
import WonTopnav from "../../components/topnav.jsx";
import WonMenu from "../../components/menu.jsx";
import WonToasts from "../../components/toasts.jsx";
import WonSlideIn from "../../components/slide-in.jsx";
import WonFooter from "../../components/footer.jsx";

import "~/style/_post.scss";
import ico_loading_anim from "~/images/won-icons/ico_loading_anim.svg";
import ico16_indicator_error from "~/images/won-icons/ico16_indicator_error.svg";
import { withRouter } from "react-router-dom";

const mapStateToProps = (state, ownProps) => {
  const { postUri } = getQueryParams(ownProps.location);
  const atomUri = postUri;
  const atom = getIn(state, ["atoms", atomUri]);

  const process = get(state, "process");
  const accountState = get(state, "account");

  return {
    isLoggedIn: accountUtils.isLoggedIn(accountState),
    atomUri,
    isOwnedAtom: generalSelectors.isAtomOwned(state, atomUri),
    atom,
    atomTitle: get(atom, "humanReadable"),
    showSlideIns:
      viewSelectors.hasSlideIns(state, ownProps.history) &&
      viewSelectors.isSlideInsVisible(state),
    showModalDialog: viewSelectors.showModalDialog(state),
    atomLoading: !atom || processUtils.isAtomLoading(process, atomUri),
    atomToLoad: !atom || processUtils.isAtomToLoad(process, atomUri),
    atomFailedToLoad:
      atom && processUtils.hasAtomFailedToLoad(process, atomUri),
  };
};

const mapDispatchToProps = dispatch => {
  return {
    fetchAtom: uri => {
      dispatch(actionCreators.atoms__fetchUnloadedAtom(uri));
    },
  };
};

class PagePost extends React.Component {
  constructor(props) {
    super(props);
    this.tryReload = this.tryReload.bind(this);
  }

  render() {
    return (
      <section className={!this.props.isLoggedIn ? "won-signed-out" : ""}>
        {this.props.showModalDialog && <WonModalDialog />}
        <WonTopnav pageTitle={this.props.atomTitle} />
        {this.props.isLoggedIn && <WonMenu />}
        <WonToasts />
        {this.props.showSlideIns && <WonSlideIn />}
        <main className="postcontent">
          {!(this.props.atomLoading || this.props.atomFailedToLoad) &&
            !!this.props.atom && <WonAtomInfo atomUri={this.props.atomUri} />}
          {this.props.atomLoading && (
            <div className="pc__loading">
              <svg className="pc__loading__spinner hspinner">
                <use xlinkHref={ico_loading_anim} href={ico_loading_anim} />
              </svg>
              <span className="pc__loading__label">Loading...</span>
            </div>
          )}
          {this.props.atomFailedToLoad && (
            <div className="pc__failed">
              <svg className="pc__failed__icon">
                <use
                  xlinkHref={ico16_indicator_error}
                  href={ico16_indicator_error}
                />
              </svg>
              <span className="pc__failed__label">
                Failed To Load - Atom might have been deleted
              </span>
              <div className="pc__failed__actions">
                <button
                  className="pc__failed__actions__button red won-button--outlined thin"
                  onClick={this.tryReload}
                >
                  Try Reload
                </button>
              </div>
            </div>
          )}
        </main>
        <WonFooter />
      </section>
    );
  }

  componentDidMount() {
    if (
      this.props.atomUri &&
      (!this.props.atom || (this.props.atomToLoad && !this.props.atomLoading))
    ) {
      this.props.fetchAtom(this.props.atomUri);
    }
  }

  componentDidUpdate(prevProps) {
    // Invoke possible fetch if:
    //    - atomUri is present AND
    //        - atomUri has changed OR
    //        - loginStatus has changed to loggedOut
    if (
      this.props.atomUri &&
      (this.props.atomUri !== prevProps.atomUri ||
        (!this.props.isLoggedIn && prevProps.isLoggedIn))
    ) {
      if (
        !this.props.atom ||
        (this.props.atomToLoad && !this.props.atomLoading)
      ) {
        this.props.fetchAtom(this.props.atomUri);
      }
    }
  }

  tryReload() {
    if (this.props.atomUri && this.props.atomFailedToLoad) {
      this.props.fetchAtom(this.props.atomUri);
    }
  }
}
PagePost.propTypes = {
  isLoggedIn: PropTypes.bool,
  atomUri: PropTypes.string,
  isOwnedAtom: PropTypes.bool,
  atom: PropTypes.object,
  atomTitle: PropTypes.string,
  showSlideIns: PropTypes.bool,
  showModalDialog: PropTypes.bool,
  atomLoading: PropTypes.bool,
  atomToLoad: PropTypes.bool,
  atomFailedToLoad: PropTypes.bool,
  fetchAtom: PropTypes.func,
};

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(PagePost)
);
