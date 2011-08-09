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


public class QueryParameters
{
  public static final String[] EMPTY_STRINGS = { };
  public static final String[] ALL_INDEXES   = { "" };

  public String   query;
  public int      start;
  public int      hitsPerPage;
  public int      hitsPerSite;
  public String[] sites       = EMPTY_STRINGS;
  public String[] indexNames  = ALL_INDEXES;
  public String[] excludes    = EMPTY_STRINGS;
  public String[] collections = EMPTY_STRINGS;
  public String[] types       = EMPTY_STRINGS;
  public String[] dates       = EMPTY_STRINGS;

  public QueryParameters()
  {
  }

  public QueryParameters( QueryParameters other )
  {
    this.query       = other.query;
    this.start       = other.start;
    this.hitsPerPage = other.hitsPerPage;
    this.hitsPerSite = other.hitsPerSite;
    this.sites       = other.sites;
    this.indexNames  = other.indexNames;
    this.excludes    = other.excludes;
    this.collections = other.collections;
    this.types       = other.types;
    this.dates       = other.dates;
  }
}
