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

package won.monitoring.simon;

import org.javasimon.Simon;

import java.io.IOException;

/**
 * Helper methods for visiting simon structures. Copied from the
 * simon-console-embed module.
 */
public class SimonVisitors {
  /**
   * Visit Simons recursively as a tree starting from the specified Simon.
   *
   * @param simon   Parent simon
   * @param visitor Visitor
   * @throws java.io.IOException
   */
  public static void visitTree(Simon simon, SimonVisitor visitor) throws IOException {
    visitor.visit(simon);
    for (Simon childSimon : simon.getChildren()) {
      visitTree(childSimon, visitor);
    }
  }
}
