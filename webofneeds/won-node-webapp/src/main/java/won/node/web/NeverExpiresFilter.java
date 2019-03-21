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

package won.node.web;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import won.protocol.vocabulary.HTTP;

/**
 * Servlet filter that adds response headers indicating that the response never expires.
 */
public class NeverExpiresFilter implements Filter
{
  //date format for Expires header (rfc 1123)
  private static final String DATE_FORMAT_RFC_1123 = "EEE, dd MMM yyyy HH:mm:ss z";


  @Override
  public void init(final FilterConfig filterConfig) throws ServletException {

  }

  /**
   * Sets the Date and Expires header fields such that the response will be treated as 'never expires'
   * (and will therefore be cached forever)
   * @return the headers map with added header values
   */
  @Override
  public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain)
    throws IOException, ServletException {
    HttpServletResponse response = (HttpServletResponse) servletResponse;
    SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_RFC_1123);
    response.addHeader(HTTP.HEADER_EXPIRES, dateFormat.format(getNeverExpiresDate()));
    response.addHeader(HTTP.HEADER_DATE, dateFormat.format(getNeverExpiresDate()));
    filterChain.doFilter(servletRequest,response);
  }

  @Override
  public void destroy() {

  }

  //Calculates a date that, according to http spec, means 'never expires'
  //See http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
  private Date getNeverExpiresDate(){
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.set(Calendar.YEAR,cal.get(Calendar.YEAR)+1);
    return cal.getTime();
  }
}
