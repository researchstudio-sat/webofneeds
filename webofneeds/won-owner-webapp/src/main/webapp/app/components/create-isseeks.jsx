import React from "react";

import "~/style/_create-isseeks.scss";
import ico16_indicator_warning from "~/images/won-icons/ico16_indicator_warning.svg";
import PropTypes from "prop-types";
import { clone } from "../utils.js";

export default class WonCreateIsSeeks extends React.Component {
  constructor(props) {
    super(props);

    const details = new Set();
    const draftObject = clone(props.initialDraft);

    for (const draftDetail in props.initialDraft) {
      details.add(draftDetail);
      draftObject[draftDetail] = props.initialDraft[draftDetail];
    }

    this.state = {
      details: details,
      openDetail: undefined,
      draftObject: draftObject,
    };
  }

  render() {
    const detailElements =
      this.props.detailList &&
      Object.values(this.props.detailList).map(detail => {
        if (detail.component) {
          const detailItemClasses = ["cis__detail__items__item"];
          this.state.openDetail === detail.identifier &&
            detailItemClasses.push("cis__detail__items__item--won-expanded");
          this.state.details.has(detail.identifier) &&
            detailItemClasses.push("cis__detail__items__item--won-hasvalue");

          const detailItemHeaderClasses = ["cis__detail__items__item__header"];
          this.state.details.has(detail.identifier) &&
            this.state.openDetail !== detail.identifier &&
            detailItemHeaderClasses.push(
              "cis__detail__items__item__header--won-showvalue"
            );
          detail.mandatory &&
            !(
              this.state.details.has(detail.identifier) ||
              this.state.openDetail === detail.identifier
            ) &&
            detailItemHeaderClasses.push(
              "cis__detail__items__item__header--won-showmandatoryindicator"
            );

          return (
            <div
              key={detail.identifier}
              className={detailItemClasses.join(" ")}
            >
              <div
                className={detailItemHeaderClasses.join(" ")}
                onClick={() => this.toggleOpenDetail(detail.identifier)}
              >
                <svg className="cis__circleicon">
                  <use xlinkHref={detail.icon} href={detail.icon} />
                </svg>
                <div className="cis__detail__items__item__header__label">
                  {detail.label}
                </div>
                {this.state.details.has(detail.identifier) &&
                  this.state.openDetail !== detail.identifier && (
                    <div className="cis__detail__items__item__header__content">
                      {this.generateHumanReadable(detail)}
                    </div>
                  )}
                {detail.mandatory &&
                  !(
                    this.state.details.has(detail.identifier) ||
                    this.state.openDetail === detail.identifier
                  ) && (
                    <div
                      className="cis__mandatory"
                      title="This is a mandatory Detail"
                    >
                      <svg className="cis__mandatory__icon">
                        <use
                          xlinkHref={ico16_indicator_warning}
                          href={ico16_indicator_warning}
                        />
                      </svg>
                    </div>
                  )}
              </div>
              {this.state.openDetail === detail.identifier && (
                <div className="cis__detail__items__item__component">
                  <detail.component
                    onUpdate={({ value }) =>
                      this.updateDetail(detail.identifier, value)
                    }
                    initialValue={this.state.draftObject[detail.identifier]}
                    detail={detail}
                  />
                </div>
              )}
            </div>
          );
        }
      });

    return (
      <won-create-isseeks>
        <div className="cis__detail__items">{detailElements}</div>
      </won-create-isseeks>
    );
  }

  generateHumanReadable(detail) {
    return detail.generateHumanReadable({
      value: this.state.draftObject[detail.identifier],
      includeLabel: false,
    });
  }

  toggleOpenDetail(detail) {
    if (this.state.openDetail === detail) {
      this.setState({ openDetail: undefined });
    } else {
      this.setState({ openDetail: detail });
    }
  }

  updateDetail(name, value) {
    const _details = this.state.details;
    const _draftObject = this.state.draftObject;
    if (value) {
      if (!_details.has(name)) {
        _details.add(name);
      }
      _draftObject[name] = value;
    } else if (_details.has(name)) {
      _details.delete(name);
      _draftObject[name] = undefined;
    }
    this.setState({
      details: _details,
      draftObject: _draftObject,
    });

    this.updateDraft();
  }

  updateDraft() {
    const _draftObject = this.state.draftObject;

    for (const detail in this.props.detailList) {
      if (!this.state.details.has(detail)) {
        _draftObject[detail] = undefined;
      }
    }
    this.setState({
      draftObject: _draftObject,
    });
    this.props.onUpdate({ draft: _draftObject });
  }
}
WonCreateIsSeeks.propTypes = {
  detailList: PropTypes.object,
  initialDraft: PropTypes.object,
  onUpdate: PropTypes.func,
};
