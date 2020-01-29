/* GROOVE: GRaphs for Object Oriented VErification
 * Copyright 2003--2007 University of Twente
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * $Id: EncodedEnabledRule.java 5780 2016-08-02 10:32:51Z rensink $
 */
package groove.explore.encode.abeona;

import groove.explore.encode.EncodedEnumeratedType;
import groove.grammar.Grammar;
import groove.grammar.QualName;
import groove.grammar.Rule;
import groove.grammar.model.GrammarModel;
import groove.grammar.model.ResourceKind;
import groove.util.parse.FormatException;

import java.util.Map;
import java.util.TreeMap;

/**
 * <!=========================================================================>
 * An EncodedEnabledRule describes an encoding of a Rule by means of a String.
 * <!=========================================================================>
 *
 * @author Maarten de Mol
 */
public class EncodedRuleParameterBinding extends EncodedEnumeratedType<RuleParameterBinding> {

    /**
     * Finds all enabled rules in the current grammar, and returns them as
     * a <String,String> mapping. The entries in this map are simply
     * <ruleName, ruleName>.
     */
    @Override
    public Map<String, String> generateOptions(GrammarModel grammar) {
        // Filter the rules that are enabled, and add them one by one to a
        // a sorted map.
        TreeMap<String, String> suitableRules = new TreeMap<>();
        for (QualName ruleName : grammar.getActiveNames(ResourceKind.RULE)) {
            final var rule = grammar.getRuleModel(ruleName);
            try {
                final var signature = rule.getSignature();
                final var pars = signature.getPars();
                for (int index = 0; index < pars.size(); index++) {
                    final var param = pars.get(index);
                    if (param.isOutOnly()) {
                        final var encoded = ruleName.toString() + "#" + index;
                        suitableRules.put(encoded, encoded);
                    }
                }
            } catch (FormatException ignored) {
            }
        }

        // Return the sorted map.
        return suitableRules;
    }

    /**
     * Attempts to finds the Rule with the given name. If such a rule does
     * not exist, or is not enabled, a FormatException is thrown.
     */
    @Override
    public RuleParameterBinding parse(Grammar rules, String bindingString) throws FormatException {
        final var pars = bindingString.split("#");
        if (pars.length != 2) {
            throw new FormatException("Invalid format: must be [qualname]#[parnum]");
        }
        final var name = pars[0];
        QualName qualName = QualName.parse(name);
        Rule rule = rules.getRule(qualName);
        if (rule == null) {
            throw new FormatException("'" + qualName + "' is not an enabled rule in the loaded grammar.");
        }
        try {
            final var paramIndex = Integer.parseInt(pars[1]);
            final var param = rule.getSignature().getPar(paramIndex);
            if (param == null) {
                throw new FormatException("'" + qualName + "' does not have a parameter at index " + paramIndex);
            }
            if (!param.isOutOnly()) {
                throw new FormatException("'" + qualName + "' does not have an out-only parameter at index " + paramIndex);
            }
            return new RuleParameterBinding(rule, paramIndex);
        } catch (NumberFormatException ex) {
            throw new FormatException("The given parameter index is not a number (given: '" + pars[1] + "')");
        }
    }
}