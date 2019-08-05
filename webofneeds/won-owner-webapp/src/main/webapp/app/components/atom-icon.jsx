/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import {get, getIn} from "../utils.js";
import {actionCreators} from "../actions/actions.js";

import * as processUtils from "../redux/utils/process-utils.js";
import * as atomUtils from "../redux/utils/atom-utils";

export default class WonAtomIcon extends React.Component {
  componentDidMount() {
    this.atomUri = this.props.atomUri;
    this.disconnect = this.props.ngRedux.connect(
      this.selectFromState.bind(this),
      actionCreators
    )(state => {
      this.setState(state);
    });
  }

  componentWillUnmount() {
    this.disconnect();
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.atomUri = nextProps.atomUri;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const atom = getIn(state, ["atoms", this.atomUri]);
    const isPersona = atomUtils.isPersona(atom);
    const image = isPersona && atomUtils.getDefaultPersonaImage(atom);

    const useCaseIcon = !isPersona
      ? atomUtils.getMatchedUseCaseIcon(atom)
      : undefined;
    const useCaseIconBackground = !isPersona
      ? atomUtils.getBackground(atom)
      : undefined;

    const identiconSvg = !useCaseIcon
      ? atomUtils.getIdenticonSvg(atom)
      : undefined;

    // Icons/Images of the AtomHolder
    const personaUri = atomUtils.getHeldByUri(atom);
    const persona = getIn(state, ["atoms", personaUri]);
    const holderImage = atomUtils.getDefaultPersonaImage(persona);
    const holderIdenticonSvg = atomUtils.getIdenticonSvg(persona);
    const showHolderIdenticon = !holderImage && holderIdenticonSvg;
    const showHolderImage = holderImage;

    const process = get(state, "process");
    return {
      isPersona,
      atomImage: isPersona && atomUtils.getDefaultPersonaImage(atom),
      atomInactive: atomUtils.isInactive(atom),
      atomFailedToLoad:
        atom && processUtils.hasAtomFailedToLoad(process, this.uri),
      useCaseIcon,
      useCaseIconBackground,
      showIdenticon: !image && identiconSvg,
      showImage: image,
      identiconSvg,
      image,
      showHolderIdenticon,
      showHolderImage,
      holderImage,
      holderIdenticonSvg,
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div/>;
    }

    let holderIcon = undefined;
    let atomIcon = undefined;

    if (this.state.showHolderIdenticon) {
      holderIcon = (
        <img className="holderIcon"
             alt="Auto-generated title image for persona that holds the atom"
             src={"data:image/svg+xml;base64," + this.state.holderIdenticonSvg}
        />
      );
    } else if (this.state.showHolderImage) {
      holderIcon = (
        <img className="holderIcon"
             alt={this.state.holderImage.get('name')}
             src={"data:" + this.state.holderImage.get('type') + ";base64," + this.state.holderImage.get('data')}
        />
      );
    }

    if (this.state.showIdenticon) {
      atomIcon = (
        <img className="image"
             alt="Auto-generated title icon"
             src={"data:image/svg+xml;base64," + this.state.identiconSvg}
        />
      );
    } else if (this.state.showImage) {
      atomIcon = (
        <img className="image"
             alt={this.state.image.get('name')}
             src={"data:" + this.state.image.get('type') + ";base64," + this.state.image.get('data')}
        />
      );
    } else if (this.state.useCaseIcon) {
      const style = {
        backgroundColor: this.state.useCaseIconBackground
      };

      atomIcon = (
        <div className="image usecaseimage" style={style}>
          <svg className="si__usecaseicon">
            <use xlinkHref="{{ ::self.useCaseIcon }}" href="{{ ::self.useCaseIcon }}"></use>
          </svg>
        </div>
      );
    }

    return (
      <won-atom-icon class={(this.state.isPersona ? " won-is-persona " : "") + (this.state.atomFailedToLoad ? " won-failed-to-load " : "") + (this.state.isInactive ? " inactive " : "")}>
        {atomIcon}
        {holderIcon}
      </won-atom-icon>
    );
  }
}