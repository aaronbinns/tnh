
import java.util.*;
import java.io.*;
import java.util.logging.Logger;

import org.apache.hadoop.io.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.*;
import org.apache.hadoop.mapred.*;
import org.apache.hadoop.mapred.lib.*;


public class Segments
{
  public static final Logger LOG = Logger.getLogger( Segments.class.getName() );

  public Map<String,Map<String,Segment>> perCollectionSegments;
  
  public Segments( String segmentsPath )
    throws IOException
  {
    Configuration conf = new Configuration();
    FileSystem fs = FileSystem.getLocal(conf);
    
    Path[] collectionDirs = getPaths( fs.listStatus(new Path(segmentsPath), getPassDirectoriesFilter(fs)) );

    if ( collectionDirs == null || collectionDirs.length == 0 )
      {
        LOG.warning( "No per-collection segment directories under: " + segmentsPath );
        return ;
      }

    for ( Path collectionDir : collectionDirs )
      {
        Path[] segmentDirs = getPaths( fs.listStatus( collectionDir, getPassDirectoriesFilter(fs) ) );

        if ( segmentDirs == null )
          {
            continue ;
          }
        
        Map<String,Segment> collectionSegments = new HashMap<String,Segment>( );
        for ( Path segmentDir : segmentDirs )
          {
            collectionSegments.put( segmentDir.getName(), new Segment( fs, segmentDir, conf ) );
          }

        perCollectionSegments.put( collectionDir.getName(), collectionSegments );
      }
  }

  public String getParseText( String collection, String segment, String key )
  {
    Map<String,Segment> collectionSegments = this.perCollectionSegments.get( collection );

    if ( collectionSegments == null )
      {
        LOG.warning( "Collection not found: " + collection );
        return "";
      }

    Segment s = collectionSegments.get( segment );

    if ( s == null )
      {
        LOG.warning( "Segment not found: " + collection + "/" + segment );
        return "";
      }
    
    try
      {
        String value = s.get( key );

        if ( value == null )
          {
            LOG.warning( "No value found for key: " + collection + "/" + segment + ": " + key );
            return "";
          }
        return value;
      }
    catch ( IOException ioe )
      {
        LOG.warning( "IOException retrieving key: " + collection + "/" + segment + ": " + key );
        return "";
      }
  }

  /* Copied from Nutch's HadoopFSUtil */
  public static Path[] getPaths( FileStatus[] stats ) 
  {
    if ( stats == null) return null;

    if (stats.length == 0) 
      {
        return new Path[0];
      }

    Path[] res = new Path[stats.length];
    for (int i = 0; i < stats.length; i++) 
      {
        res[i] = stats[i].getPath();
      }
    return res;
  }

  public static PathFilter getPassDirectoriesFilter( final FileSystem fs )
  {
    return new PathFilter()
      {
        public boolean accept(final Path path)
        {
          try
            {
              return fs.getFileStatus(path).isDir();
            }
          catch (IOException ioe)
            {
              return false;
            }
        }
    };
  }

}

class Segment
{
  public static final Partitioner PARTITIONER = new HashPartitioner();

  public MapFile.Reader[] parseTextReaders;

  public Segment( FileSystem fs, Path segmentDir, Configuration conf)
    throws IOException
  {
    this.parseTextReaders = MapFileOutputFormat.getReaders(fs, new Path(segmentDir, "parse_text"), conf);
  }

  public String get( String key ) throws IOException
  {
    Writable w = MapFileOutputFormat.getEntry( this.parseTextReaders, PARTITIONER, new Text(key), new ParseText() );

    if ( w == null ) return null;

    return w.toString();
  }
  
}


class ParseText implements Writable
{
  private final static byte VERSION = 2;

  public ParseText() {}
  private String text;
    
  public ParseText(String text){
    this.text = text;
  }

  public void readFields(DataInput in) throws IOException {
    byte version = in.readByte();
    switch (version) {
    case 1:
      text = WritableUtils.readCompressedString(in);
      break;
    case VERSION:
      text = Text.readString(in);
      break;
    default:
      throw new VersionMismatchException(VERSION, version);
    }
  }

  public final void write(DataOutput out) throws IOException {
    out.write(VERSION);
    Text.writeString(out, text);
  }

  public final static ParseText read(DataInput in) throws IOException {
    ParseText parseText = new ParseText();
    parseText.readFields(in);
    return parseText;
  }

  //
  // Accessor methods
  //
  public String getText()  { return text; }

  public boolean equals(Object o) {
    if (!(o instanceof ParseText))
      return false;
    ParseText other = (ParseText)o;
    return this.text.equals(other.text);
  }

  public String toString() {
    return text;
  }

}
