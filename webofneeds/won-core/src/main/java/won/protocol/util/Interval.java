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

package won.protocol.util;

import java.util.Date;

public class Interval
{
  final Date from;
  final Date to;

  public Interval(final Date from, final Date to)
  {
    if (from == null) {
      if (to == null) throw new IllegalArgumentException("At least one date must be specified!");
      this.from = new Date(0);
      this.to = to;
    } else if (to == null) {
      this.from = from;
      this.to = new Date(Long.MAX_VALUE);
    } else if (from.after(to)) {
      this.from = to;
      this.to = from;
    } else {
      this.to = to;
      this.from = from;
    }
  }
  public Date getFrom() {
    return from;
  }

  public Date getTo() {
    return to;
  }

}