"The New Hotness" started as an experiment to prototype a Lucene
TopDocCollector that collapses results based on the 'site' field as
the documents are scored, rather than collapsing after the results
collected.

The CollapsingCollector implements this approach successfully.

Once that class was developed, an OpenSearch web service was built, as
well as metasearch across multiple OpenSearch servers.

The last piece was the ability to read Nutch segments for
snippetizing, thus enabling use of NutchWAX-built index+segment
shards.
