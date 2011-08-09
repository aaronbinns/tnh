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

import org.apache.lucene.search.highlight.Encoder;

/**
 * Like the SimpleHTMLEncoder bundled with Lucene, but this one isn't
 * broken.
 */
public class NonBrokenHTMLEncoder implements Encoder
{
  public String encodeText( String originalText )
  {
    if ( originalText == null ) return originalText;
    
    originalText = originalText.replaceAll( "[&]", "&amp;" );
    originalText = originalText.replaceAll( "[<]", "&lt;"  );

    return originalText;
  }
}
