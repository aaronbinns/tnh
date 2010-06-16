The New Hotness
2010-06-16

The New Hotness (TNH) is a (near) drop-in replacement for
NutchWAX-based search services.  It is primarily intended to be used
internally at Internet Archive, but may be of use/interest to other
NutchWAX users.

TNH started as an experiment to prototype a Lucene TopDocCollector
that collapses results based on the 'site' field as the documents are
scored, rather than collapsing after the results collected.  The
result is the CollapsingCollector class.

Once that class was developed, an OpenSearch web service was built, as
well as metasearch across multiple OpenSearch servers.

The last piece was the ability to read Nutch segments for
snippetizing, thus enabling use of NutchWAX-built index+segment
shards.
