/**
 * Also a resizing textfield that can produce messages but it only uses 
 * a standard text-area instead of contenteditable. Thus it should be 
 * stabler but can't do rich text / wysiwyg-formatting.
 * 
 * Created by ksinger on 16.02.2018.
 */

;

// import Medium from '../mediumjs-es6.js';
import angular from 'angular';
// import 'ng-redux';
import Immutable from 'immutable';
import 'angular-sanitize';
import {
    dispatchEvent,
    attach,
    delay,
    is,
} from '../utils.js';
import { actionCreators }  from '../actions/actions.js';
import autoresizingTextareaModule from '../directives/textarea-autogrow.js';

function genComponentConf() {
    let template = `
        <textarea 
            won-textarea-autogrow
            data-min-rows="1"
            data-max-rows="4"

            class="wdt__text"
            ng-class="{'wdt__text--is-code': self.isCode, 'valid' : self.belowMaxLength(), 'invalid' : !self.belowMaxLength() }"
            tabindex="0"
            placeholder="{{self.placeholder}}"></textarea>

        <button
            class="wdt__submitbutton red"
            ng-show="self.submitButtonLabel"
            ng-click="self.submit()"
            ng-disabled="!self.valid()">
            {{ (self.submitButtonLabel || 'Submit') }}
        </button>

        <div class="wdt__charcount" ng-show="self.maxChars">
            {{ self.charactersLeft() }} characters left
        </div>

        <div class="wdt__helptext" ng-show="self.helpText">
            {{ self.helpText }}
        </div>
    `;

    const serviceDependencies = ['$scope', '$element', /*'$ngRedux',/*injections as strings here*/];

    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);
            window.ctfs4dbg = this;

            /*
            const selectFromState = (state) => ({
                draftId: state.getIn(['router', 'currentParams', 'draftId'])
            })
            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
            */

            this.textFieldNg().bind('input', e => {
                this.input()
                return false;
            });
            this.textFieldNg().bind('paste', e => {
                this.paste()
            });
            this.textFieldNg().bind('keydown', e => {
                this.keydown(e)
                return false;
            });
        }
        keydown(e) {
            if(e.keyCode === 13 && !e.shiftKey) {
                e.preventDefault(); // prevent a newline from being entered
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

            /* trigger digest so button and counter update
             * delay is because submit triggers an input-event
             * and is in a digest-cycle already. opposed to user-
             * triggered input-events. dunno why the latter doesn't
             * do that tho.
             */
            delay(0).then(() => 
                this.$scope.$digest() 
            )
        }
        submit() {
            const value = this.value();
            const valid = this.valid();
            if(valid) {
                const txtEl = this.textField();
                if(txtEl) {
                    txtEl.value = "";
                    txtEl.dispatchEvent(new Event('input')); // dispatch input event so autoresizer notices value-change
                    txtEl.focus(); //refocus so people can keep writing
                }
                const payload = { value, valid };
                this.onSubmit(payload);
                dispatchEvent(this.$element[0], 'submit', payload);
            }
        }
        charactersLeft() {
            return this.maxChars - this.value().length;
        }
        belowMaxLength() {
            return !this.maxChars || this.charactersLeft() >= 0;
        }
        valid() {
            return (this.allowEmptySubmit || this.value().length > 0) && this.belowMaxLength()
        }
        value() {
            const txtEl = this.textField();
            if(txtEl) {
                return txtEl.value.trim();
            }
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
            placeholder: '=', // NOTE: bound only once
            maxChars: '=',
            helpText: '=',

            isCode: '=', // whether or not the text is code and e.g. should use monospace

            allowEmptySubmit: '=', // allows submitting empty messages

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


export default angular.module('won.owner.components.chatTextfieldSimple', [ 
    autoresizingTextareaModule,
])
    .directive('chatTextfieldSimple', genComponentConf)
    .name;