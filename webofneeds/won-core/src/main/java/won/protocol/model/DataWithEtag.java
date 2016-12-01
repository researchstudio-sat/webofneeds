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

/**
 * Data holder for situations in which data is used together with a value that can be used for an
 * http ETAG header.
 * @param <T>
 */
public class DataWithEtag<T>
{
  private T data;
  private String etag;
  private String oldEtag;

  public static DataWithEtag dataNotFound(){
    return new DataWithEtag(null, null, null);
  }

  /**
   * Creates a DWE that indicates nothing has changed. May be useful if the type of the
   * object at hand has the wrong generic type.
   * @param data
   * @return
   */
  public static DataWithEtag dataNotChanged(DataWithEtag data) {
    return new DataWithEtag(null, data.etag, data.oldEtag);
  };

  public DataWithEtag(final T data, final String etag, final String oldEtag) {
    this.data = data;
    this.etag = etag;
    this.oldEtag = oldEtag;
  }

  public T getData() {
    return data;
  }

  /**
   * The etag currently associated with the data.
   * @return
   */
  public String getEtag() {
    return etag;
  }

  /**
   * Returns true if this DataWithEtag object is the result of an ETAG checking call and the
   * data has changed since.
   *
   * This is the case if
   * @return
   */
  public boolean isChanged() {
    if (etag == null || oldEtag == null){
      //no etag now? no matter what the old etag was, assume the data has changed
      //no old etag, but new one? there was a change, too!
      return true;
    }
    //both etags are there: compare them no change if they are equal
    return ! etag.equals(oldEtag);
  }

  /**
   * If the data is null, there are two options:
   * * the etags are identical, so there was no change - therefore the data was not populated
   * * the data was not found, in which case the new etag is null, and even if the old etag is null, too, we
   *   treat this as a change.
   *
   * This method allows querying if the data is null because it was not found.
   * @return
   */
  public boolean isNotFound(){
    return isChanged() && data == null;
  }
}
