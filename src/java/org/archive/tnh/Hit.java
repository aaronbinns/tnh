/*
 * Copyright 2010 Internet Archive
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.archive.tnh;

/**
 * Simple structure to hold a document ID, score and site.
 */
public class Hit implements Comparable<Hit>
{
  public int    id;
  public float  score;
  public String site;
  
  public Hit( int id, float score, String site )
  {
    this.id    = id;
    this.score = score;
    this.site  = site;
  }

  public int compareTo( Hit that )
  {
    if ( this.score < that.score ) return -1;
    if ( this.score > that.score ) return  1;

    if ( this.id < that.id ) return -1;
    if ( this.id > that.id ) return  1;

    return 0;
  }
}
