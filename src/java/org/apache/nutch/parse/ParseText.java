
package org.apache.nutch.parse;

import java.io.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.*;


public class ParseText implements Writable
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
