
import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.util.Version;


/**
 * Simple custom analyzer that combines a bunch of common filters.
 * This would be nice to have in an external config file, but for
 * now we just have this little hard-coded class.
 */
public class CustomAnalyzer extends Analyzer
{
  boolean foldAccents  = false;
  boolean omitNonAlpha = false;
  Set<?> stopWords;

  public CustomAnalyzer( )
  {

  }

  public void setStopWords( Set<?> stopWords )
  {
    this.stopWords = stopWords;
  }

  public Set<?> getStopWords( )
  {
    return this.stopWords;
  }

  public void setOmitNonAlpha( boolean omitNonAlpha )
  {
    this.omitNonAlpha = omitNonAlpha;
  }

  public boolean getOmitNonAlpha( )
  {
    return this.omitNonAlpha;
  }

  public void setFoldAccents( boolean foldAccents )
  {
    this.foldAccents = foldAccents;
  }

  public boolean getFoldAccents( )
  {
    return this.foldAccents;
  }

  public TokenStream tokenStream( String fieldName, Reader reader )
  {
    TokenStream stream = new StandardTokenizer( Version.LUCENE_30, reader );
    stream = new StandardFilter( stream );
    stream = new LowerCaseFilter( stream );
    if ( this.foldAccents )
      {
        stream = new ASCIIFoldingFilter( stream );
      }
    if ( this.omitNonAlpha )
      {
        stream = new NonAlphaFilter( stream );
      }
    if ( this.stopWords != null && this.stopWords.size( ) > 0 )
      {
        stream = new StopFilter( true, stream, this.stopWords );
      }

    return stream;
  }

}
