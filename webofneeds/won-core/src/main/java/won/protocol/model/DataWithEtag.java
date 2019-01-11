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
  //if true indicates that the data was not found (as opposed to  unchanged)
  private boolean notFound = false;
  private boolean isDeleted = false;


  public static DataWithEtag dataNotFound(){
    return new DataWithEtag(null, null, null, true, false);
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

  /**
   * Creates a DWE that indicates nothing has changed.
   * @param etag the unchanged etag
   * @return
   */
  public static DataWithEtag dataNotChanged(String etag) {
    return new DataWithEtag(null, etag, etag);
  }

  public DataWithEtag(final T data, final String etag, String oldEtag, boolean notFound, boolean isDeleted) {
    this.data = data;
    this.etag = etag;
    this.oldEtag = oldEtag;
    this.notFound = notFound;
    this.isDeleted = isDeleted;
  }

  /**
   * Construcutor setting notFound + isDeleted to false.
   * @param data
   * @param etag
   */
  public DataWithEtag(final T data, final String etag, final String oldEtag) {
    this(data, etag, oldEtag, false, false);
  }

  /**
   * Construcutor setting notFound false + isDeleted to value.
   * @param data
   * @param etag
   * @param isDeleted
   */
  public DataWithEtag(final T data, final String etag, final String oldEtag, final boolean isDeleted) {
    this(data, etag, oldEtag, false, isDeleted);
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
   * 
   * This method allows querying if the data is deleted. Only returns true
   * if isDeleted was explicitly set to true before.
   * @return
   */
  public boolean isDeleted() {
      return this.isDeleted;
    }

  /**
   *
   * This method allows querying if the data is null because it was not found. Only returns true
   * if notFound was explicitly set to true before.
   * @return
   */
  public boolean isNotFound(){
    return notFound;
  }
}
