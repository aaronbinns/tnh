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

import java.net.*;


public class TestResource
{
  public static void main( String[] args )
    throws Exception
  {
    String location = args[0];

    // Look for the resource via the classloader, but if not found,
    // then just try it as a regular URL.
    URL u = ServletHelper.class.getClassLoader().getResource( location );
    
    if ( u != null )
      {
        System.out.println( "Found in classpath: " + u );
        return;
      }

    // Use the current working directory as the context fo the
    // location.  This way, if the location is a relative file path,
    // it will be found relative to the CWD.  If the location is a
    // full path, the full path will be used.  If the location is a
    // URL (i.e. has a scheme) that will be used and the context is
    // ignored.
    u = new URL( new URL( "file://" + System.getProperty( "user.dir" ) + "/" ), location );

    System.out.println( "URL: " + u );

    return;
  }
}
