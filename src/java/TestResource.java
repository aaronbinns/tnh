
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