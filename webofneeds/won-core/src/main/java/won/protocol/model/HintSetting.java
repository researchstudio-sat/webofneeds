/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.protocol.model;

import won.protocol.vocabulary.WON;

import java.net.URI;

/**
 * Specifies, for one of the two sides of a match, if it should receive hints from a matcher.
 * Each need can specify one of these settings for itself ('need') and for the matched need ('counterpart').
 *
 * There are three types:
 * Yes: that party should receive hints
 * No: that party should not receive hints
 * WeakYes: that party should receive hints unless the other uses No
 *
 * If no type is specified, WeakYes is assumed.
 *
 * The setting for 'need' overrides the setting that the matched need has for 'counterpart', therefore
 * <table>
 *     <tr>
 *         <td colspan="5" align="center"> need </td>
 *     </tr>
 *     <tr>
 *         <td></td></td><td></td><td>Yes</td><td>WeakYes</td><td>No</td>
 *     </tr>
 *     <tr>
 *         <td>counterpart</td><td>Yes</td><td>send</td><td>send</td><td>-</td>
 *     </tr>
 *     <tr>
 *         <td></td><td>WeakYes</td><td>send</td><td>send</td><td>-</td>
 *     </tr>
 *     <tr>
 *         <td></td><td>No</td><td>send</td><td>-</td><td>-</td>
 *     </tr>
 * </table>
 */
public enum HintSetting {
    YES("Yes"), WEAK_YES("WeakYes"), NO("No");

    public static final HintSetting DEFAULT = WEAK_YES;

    private String name;

    HintSetting(String name) {
        this.name = name;
    }



    public static boolean shouldSendHint(HintSetting needSetting, HintSetting counterpartSetting){
        if (needSetting == YES) return true;
        if (needSetting == NO) return false;
        if (needSetting == WEAK_YES) return counterpartSetting != NO;
        throw new IllegalArgumentException("could not determine whether to send a hint or not for 'need' HintSetting" + needSetting + " and 'counterpart' HintSetting " + counterpartSetting);
    }

    public URI getURI()
    {
        return URI.create(WON.BASE_URI + name);
    }

    /**
     * Tries to match the given URI against all enum values.
     *
     * @param uri URI to match
     * @return matched enum, null otherwise
     */
    public static HintSetting fromURI(final URI uri)
    {
        for (HintSetting type : values())
            if (type.getURI().equals(uri))
                return type;
        return null;
    }

    /**
     * Tries to match the given URI against all enum values.
     *
     * @param uri URI to match
     * @return matched enum, null otherwise
     */
    public static HintSetting fromURI(final String uri)
    {
        for (HintSetting type : values())
            if (type.getURI().toString().equals(uri))
                return type;
        return null;
    }
}
