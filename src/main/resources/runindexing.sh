#!/bin/bash

java -Xmx8G -ea -server \
\
-Dterrier.etc=/Users/khast/code/terrier/succinct/src/main/resources \
\
it.cnr.isti.hpclab.succinct.QuasiSuccinctIndexGenerator /Users/khast/index-java cw09b cw09b.sux