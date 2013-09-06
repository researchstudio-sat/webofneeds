/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package won.matcher.query.rdf.algebra.expr.library.util;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.sparql.engine.binding.BindingFactory;
import com.hp.hpl.jena.sparql.expr.Expr;
import com.hp.hpl.jena.sparql.expr.NodeValue;
import com.hp.hpl.jena.sparql.function.FunctionEnvBase;
import org.sindice.siren.search.SirenNumericRangeQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * User: fkleedorfer
 * Date: 06.09.13
 */
public class NumericRangeUtils
{

  private static final String DEFAULT_HANDLER = "DEFAULT_HANDLER";
  private Map<String, NumericRangeHandler> handlerMap = new HashMap<String, NumericRangeHandler>();
  private static NumericRangeUtils instance = new NumericRangeUtils(4,4,4,4);
  private final Logger logger = LoggerFactory.getLogger(getClass());

  public static NumericRangeUtils getInstance(){
    return instance;
  }

  private NumericRangeUtils(final int precisionStepInt, final int precisionStepDouble, final int precisionStepFloat, final int precisionStepLong)
  {
    NumericRangeHandler intHandler = new IntRangeHandler(precisionStepInt);
    NumericRangeHandler doubleHandler = new DoubleRangeHandler(precisionStepDouble);
    NumericRangeHandler floatHandler = new FloatRangeHandler(precisionStepFloat);
    NumericRangeHandler longHandler = new LongRangeHandler(precisionStepLong);
    this.handlerMap.put(XSDDatatype.XSDint.getURI().toString(),intHandler);
    //seems xsd:integer is meant to map to Bigint.. but pragmatically, we use Integer
    this.handlerMap.put(XSDDatatype.XSDinteger.getURI().toString(),intHandler);
    this.handlerMap.put(XSDDatatype.XSDdouble.getURI().toString(),doubleHandler);
    this.handlerMap.put(XSDDatatype.XSDfloat.getURI().toString(),floatHandler);
    this.handlerMap.put(XSDDatatype.XSDlong.getURI().toString(),longHandler);
    this.handlerMap.put(DEFAULT_HANDLER, new UntypedStringRangeHandler());
  }

  public SirenNumericRangeQuery newLessThanRange(String field, Expr thresholdExpr, boolean includeThreshold){
    logger.debug("newLessThanRange for expr:'{}', includeThreshold:{}", thresholdExpr, includeThreshold);
    NodeValue nv = evaluate(thresholdExpr);
    return getHandler(nv).newLessThanRange(field, nv,includeThreshold);
  }

  public SirenNumericRangeQuery newGreaterThanRange(String field, Expr thresholdExpr, boolean includeThreshold){
    logger.debug("newGreaterThanRange for expr:'{}', includeThreshold:{}", thresholdExpr, includeThreshold);
    NodeValue nv = evaluate(thresholdExpr);
    return getHandler(nv).newGreaterThanRange(field, nv, includeThreshold);
  }

  private NumericRangeHandler getHandler(NodeValue nv){
    String datatypeURI = nv.getDatatypeURI();
    logger.debug("fetching handler for node value '{}' with datatype URI {}", nv, datatypeURI);
    if (datatypeURI == null) return this.handlerMap.get(DEFAULT_HANDLER);
    NumericRangeHandler handler = this.handlerMap.get(datatypeURI);
    logger.debug("found handler '{}'", handler);
    if (handler != null) return handler;
    return this.handlerMap.get(DEFAULT_HANDLER);
  }

  /**
   * Assumes no variables inside expression, evaluates with empty binding and new function environment.
   * @param expression
   * @return
   */
  private NodeValue evaluate(Expr expression) {
    return expression.eval(BindingFactory.binding(), new FunctionEnvBase());
  }



  private interface NumericRangeHandler
  {
    public SirenNumericRangeQuery newLessThanRange(final String field, NodeValue thresholdValue, boolean inclusive);
    public SirenNumericRangeQuery newGreaterThanRange(final String field, NodeValue thresholdValue, boolean inclusive);
  }

  private class IntRangeHandler implements NumericRangeHandler {
    private int precisionStep = 4;

    private IntRangeHandler(final int precisionStep)
    {
      this.precisionStep = precisionStep;
    }

    @Override
    public SirenNumericRangeQuery newLessThanRange(final String field, final NodeValue thresholdValue, final boolean inclusive)
    {
      return SirenNumericRangeQuery.newIntRange(field, precisionStep, Integer.MIN_VALUE, Integer.parseInt(thresholdValue.asUnquotedString()),true,inclusive);
    }

    @Override
    public SirenNumericRangeQuery newGreaterThanRange(final String field, final NodeValue thresholdValue, final boolean inclusive)
    {
      return SirenNumericRangeQuery.newIntRange(field, precisionStep, Integer.parseInt(thresholdValue.asUnquotedString()), Integer.MAX_VALUE,inclusive, true);
    }
  }

  private class DoubleRangeHandler implements NumericRangeHandler {
    private int precisionStep = 4;

    private DoubleRangeHandler(final int precisionStep)
    {
      this.precisionStep = precisionStep;
    }

    @Override
    public SirenNumericRangeQuery newLessThanRange(final String field, final NodeValue thresholdValue, final boolean inclusive)
    {
      return SirenNumericRangeQuery.newDoubleRange(field, precisionStep, -Double.MAX_VALUE, Double.parseDouble(thresholdValue.asUnquotedString()), true, inclusive);
    }

    @Override
    public SirenNumericRangeQuery newGreaterThanRange(final String field, final NodeValue thresholdValue, final boolean inclusive)
    {
      return SirenNumericRangeQuery.newDoubleRange(field, precisionStep, Double.parseDouble(thresholdValue.asUnquotedString()), Double.MAX_VALUE, inclusive, true);
    }
  }


  private class FloatRangeHandler implements NumericRangeHandler {
    private int precisionStep = 4;

    private FloatRangeHandler(final int precisionStep)
    {
      this.precisionStep = precisionStep;
    }

    @Override
    public SirenNumericRangeQuery newLessThanRange(final String field, final NodeValue thresholdValue, final boolean inclusive)
    {
      return SirenNumericRangeQuery.newFloatRange(field, precisionStep, -Float.MAX_VALUE, Float.parseFloat(thresholdValue.asUnquotedString()), true, inclusive);
    }

    @Override
    public SirenNumericRangeQuery newGreaterThanRange(final String field, final NodeValue thresholdValue, final boolean inclusive)
    {
      return SirenNumericRangeQuery.newFloatRange(field, precisionStep, Float.parseFloat(thresholdValue.asUnquotedString()), Float.MAX_VALUE, inclusive, true);
    }
  }

  private class LongRangeHandler implements NumericRangeHandler {
    private int precisionStep = 4;

    private LongRangeHandler(final int precisionStep)
    {
      this.precisionStep = precisionStep;
    }

    @Override
    public SirenNumericRangeQuery newLessThanRange(final String field, final NodeValue thresholdValue, final boolean inclusive)
    {
      return SirenNumericRangeQuery.newLongRange(field, precisionStep, Long.MIN_VALUE, Long.parseLong(thresholdValue.asUnquotedString()), true, inclusive);
    }

    @Override
    public SirenNumericRangeQuery newGreaterThanRange(final String field, final NodeValue thresholdValue, final boolean inclusive)
    {
      return SirenNumericRangeQuery.newLongRange(field, precisionStep, Long.parseLong(thresholdValue.asUnquotedString()), Long.MAX_VALUE, inclusive, true);
    }
  }

  /**
   * Tries to interpret the specified string as long, then int, then double, then float, then gives up.
   * If one of these checks is successful, the call is delegated to the appropriate handler.
   * If none of the checks succeeds, null is returned.
   */
  private class UntypedStringRangeHandler implements NumericRangeHandler {
      private int precisionStep = 4;

    private UntypedStringRangeHandler()
    {
    }

    @Override
    public SirenNumericRangeQuery newLessThanRange(final String field, final NodeValue thresholdValue, final boolean inclusive)
    {
      String stringValue = thresholdValue.asUnquotedString();
      NumericRangeHandler handler = getDatatypeHandlerForValue(stringValue);
      if (handler == null) return null;
      return handler.newLessThanRange(field, thresholdValue, inclusive);
    }

    @Override
    public SirenNumericRangeQuery newGreaterThanRange(final String field, final NodeValue thresholdValue, final boolean inclusive)
    {
      String stringValue = thresholdValue.asUnquotedString();
      NumericRangeHandler handler = getDatatypeHandlerForValue(stringValue);
      if (handler == null) return null;
      return handler.newGreaterThanRange(field, thresholdValue, inclusive);
    }

    private NumericRangeHandler getDatatypeHandlerForValue(String stringValue){
      try {
        Long.parseLong(stringValue);
        return handlerMap.get(XSDDatatype.XSDlong.getURI());
      } catch (NumberFormatException e){}
      try {
        Integer.parseInt(stringValue);
        return handlerMap.get(XSDDatatype.XSDint.getURI());
      } catch (NumberFormatException e){}
      try {
        Double.parseDouble(stringValue);
        return handlerMap.get(XSDDatatype.XSDdouble.getURI());
      } catch (NumberFormatException e){}
      try {
        Float.parseFloat(stringValue);
        return handlerMap.get(XSDDatatype.XSDfloat.getURI());
      } catch (NumberFormatException e){}
      return null;
    }
  }
}
