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

package won.matcher.service;

/**
 * Calculates a bounded linear function of the input score. The score is mapped from the interval in the
 * domain (specified by domainMin and domainMax) to the interval in the range (rangeMin and rangeMax). Values
 * outside the domain interval treated as if they were on the interval boundary.
 *
 * The inputThreshold, which should be higher than domainMin, is used to cut off the input at a threshold value.
 * Input values below the inputThreshold will be treated as if they were <= domainMin.
 *
 */
public class ScoreTransformer
{
  private float inputThreshold = 0.2f;
  private float rangeMin = 0.0f;
  private float rangeMax = 1.0f;
  private float domainMin = 0f;
  private float domainMax = 10f;

  public ScoreTransformer()
  {
  }

  public ScoreTransformer(final float inputThreshold, final float rangeMin, final float rangeMax, final float domainMin, final float domainMax)
  {
    this.inputThreshold = inputThreshold;
    this.rangeMin = rangeMin;
    this.rangeMax = rangeMax;
    this.domainMin = domainMin;
    this.domainMax = domainMax;
  }

  public boolean isAboveInputThreshold(final float score){
    return score >= inputThreshold;
  }

  public float transform(final float score){
    float myScore = isAboveInputThreshold(score)?score:domainMin;
    return (Math.min(Math.max(myScore - domainMin,0),1) / getDomainSpan()) * getRangeSpan() + rangeMin;
  }

  public float getDomainSpan(){
    return domainMax - domainMin;
  }

  public float getRangeSpan(){
    return rangeMax - rangeMin;
  }

  public float getInputThreshold()
  {
    return inputThreshold;
  }

  public void setInputThreshold(final float inputThreshold)
  {
    this.inputThreshold = inputThreshold;
  }

  public float getRangeMin()
  {
    return rangeMin;
  }

  public void setRangeMin(final float rangeMin)
  {
    this.rangeMin = rangeMin;
  }

  public float getRangeMax()
  {
    return rangeMax;
  }

  public void setRangeMax(final float rangeMax)
  {
    this.rangeMax = rangeMax;
  }

  public float getDomainMin()
  {
    return domainMin;
  }

  public void setDomainMin(final float domainMin)
  {
    this.domainMin = domainMin;
  }

  public float getDomainMax()
  {
    return domainMax;
  }

  public void setDomainMax(final float domainMax)
  {
    this.domainMax = domainMax;
  }
}
