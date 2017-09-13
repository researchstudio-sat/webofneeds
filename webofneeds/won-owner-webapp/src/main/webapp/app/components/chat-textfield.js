/**
 * Created by ksinger on 17.06.2016.
 */

;

// import Medium from '../mediumjs-es6.js';
import angular from 'angular';
import 'ng-redux';
import Immutable from 'immutable';
import 'angular-sanitize';
import {
    dispatchEvent,
    attach,
    delay,
    is,
} from '../utils.js';
import { actionCreators }  from '../actions/actions.js';

function genComponentConf() {
    let template = `
        <div class="wdt__left">
            <div class="wdt__text"
                    ng-class="{ 'valid' : self.valid(), 'invalid' : !self.valid() }">
                <div class="medium-mount" tabindex="0"></div>
            </div>
            <span class="wdt__charcount" ng-show="self.maxChars">
                {{ self.charactersLeft() }} characters left
            </span>
        </div>
        <button
            class="wdt__submitbutton red"
            ng-show="::self.submitButtonLabel"
            ng-click="::self.submit()">
            {{ ::(self.submitButtonLabel || 'Send') }}
        </button>
    `;

    const serviceDependencies = ['$scope', '$element', '$ngRedux',/*injections as strings here*/];

    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);
            window.ctf4dbg = this;

            /*
            const selectFromState = (state) => ({
                draftId: state.getIn(['router', 'currentParams', 'draftId'])
            })
            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
            */

            this.initMedium();

            this.mediumMountNg().bind('input', e => {
                this.input()
            });
            this.mediumMountNg().bind('paste', e => {
                this.paste()
            });
            this.mediumMountNg().bind('keydown', e =>
                this.keydown(e)
            );
        }
        keydown(e) {
            if(e.keyCode === 13) {
                this.submit();
                return false;
            }
        }
        paste() {
            const payload = {
                value: this.value(),
                valid: this.valid(),
            };
            this.onPaste(payload);
            dispatchEvent(this.$element[0], 'paste', payload);
        }
        input() {
            const payload = {
                value: this.value(),
                valid: this.valid(),
            };
            this.onInput(payload);
            dispatchEvent(this.$element[0], 'input', payload);
        }
        submit() {
            const value = this.value();
            const valid = this.valid();
            if(value && valid) {
                this.medium.clear(); // clear text
                this.mediumMount().focus(); //refocus so people can keep writing

                const payload = { value, valid };
                this.onSubmit(payload);
                dispatchEvent(this.$element[0], 'submit', payload);
            }
        }
        charactersLeft() {
            return this.maxChars - this.value().length;
        }
        valid() {
            return !this.maxChars || this.charactersLeft() >= 0;
        }
        value() {
            return this.medium
                .value()
                /*
                 * the replace fixes odd behaviour of FF. it inserts
                 * a `<br>` at the end after the first space is
                 * typed -- unless the space is the first character
                 * in the field.
                 */
                .replace(/<br>$/, '')
                .replace('&nbsp;','')
                .trim();
        }

        initMedium() {
            // initialising editor. see http://jakiestfu.github.io/Medium.js/docs/
            this.medium = new Medium({
                element: this.mediumMount(),

                modifier: 'auto',

                placeholder: is('string', this.placeholder) ?
                    // make sure we've got a string to avoid errors internal to medium.js
                    this.placeholder : "",

                autoHR: false, //if true, inserts <hr> after two empty lines
                mode: Medium.inlineMode, // no newlines, no styling
                //mode: Medium.partialMode, // allows newlines, no styling
                //maxLength: this.maxChars, // -1 would disable it
                tags: {
                    /*
                     'break': 'br',
                     'horizontalRule': 'hr',
                     'paragraph': 'p',
                     'outerLevel': ['pre', 'blockquote', 'figure'],
                     'innerLevel': ['a', 'b', 'u', 'i', 'img', 'strong']
                     */
                },
                attributes: {
                    //remove: ['style', 'class'] //TODO does this remove the ng-class?
                    remove: ['style'] //TODO does this remove the ng-class?
                },
            });

            //remove the inline-styles placed by medium.js
            this.medium.placeholder.style = "";
            this.mediumMount().addEventListener('blur', e =>
                delay(0) //push to end end of task-queue (and thus all other `blur`-listeners
                    .then(() => {
                        const style = this.medium.placeholder.style;
                        style.minHeight = 0;
                        style.minWidth = 0;
                    })
            );
        }

        mediumMountNg() {
            return angular.element(this.mediumMount());
        }

        mediumMount() {
            if(!this._mediumMount) {
                this._mediumMount = this.textField().querySelector('.medium-mount')
            }
            return this._mediumMount;
        }

        textFieldNg() {
            return angular.element(this.textField())
        }
        textField() {
            if(!this._textField) {
                this._textField = this.$element[0].querySelector('.wdt__text');
            }
            return this._textField;
        }
    }
    Controller.$inject = serviceDependencies;


    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            placeholder: '=',
            maxChars: '=',
            /*
             * Usage:
             *  on-input="::myCallback(value, valid)"
             */
            onInput: '&',
            /*
             * Usage:
             *  on-paste="::myCallback(value, valid)"
             */
            onPaste: '&',

            submitButtonLabel: '=',
            /*
             * Usage:
             *  on-submit="::myCallback(value)"
             */
            onSubmit: '&',

        },
        template: template
    }
}


export default angular.module('won.owner.components.chatTextfield', [ ])
    .directive('chatTextfield', genComponentConf)
    .name;