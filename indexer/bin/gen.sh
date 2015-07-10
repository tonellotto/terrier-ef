#!/usr/bin/env bash

# Attempt to set JAVA_HOME if it is not set
if [[ -z $JAVA_HOME ]]; then
  # On OSX use java_home (or /Library for older versions)
  if [ "Darwin" == "$(uname -s)" ]; then
    if [ -x /usr/libexec/java_home ]; then
      export JAVA_HOME=($(/usr/libexec/java_home))
    else
      export JAVA_HOME=(/Library/Java/Home)
    fi
  fi

  # Bail if we did not detect it
  if [[ -z $JAVA_HOME ]]; then
    echo "Error: JAVA_HOME is not set and could not be found." 1>&2
    exit 1
  fi
fi

JAVA=$JAVA_HOME/bin/java
# some Java parameters
JAVA_HEAP_MAX=-Xmx8G

bin=`which $0`
bin=`dirname ${bin}`
bin=`cd "$bin"; pwd`

function print_usage(){
  echo "Usage: gen.sh PATH PREFIX"
  echo "where"
  echo "  PATH		is the source and resulting index path (must be absolute)"
  echo "  PREFIX  	is the source index prefix, the resulting index path will be PATH.sux"
}

if [ $# = 0 ]; then
  print_usage
  exit
fi

if [ $1 == "-h" -o $1 == "-help"  -o $1 == "--help" -o $# != 2 ]; then
	print_usage
    exit
fi

PATH=$1
shift
PREFIX=$1
shift

if [[ ! -e $PATH ]]; then
	echo "Error: directory ${PATH} does not exist... Aborting"
	exit
elif [[ -f $PATH ]]; then
	echo "Error: directory ${PATH} is actually a file... Aborting"
	exit
elif [[ $PATH != /* ]]; then
	echo "Error: directory ${PATH} must be absolute... Aborting"
	exit
elif [[ -e ${PATH}"/"${PREFIX}".sux.properties" ]]; then
	echo "Warning: directory ${PATH} seems to already contain an index with prefix ${PREFIX}.sux... Aborting" 
	exit
fi

basedir=${bin%/*}
if [[ ! -e ${basedir}"/target/terrier-eliasfano-indexer-2.0-jar-with-dependencies.jar" ]]; then
	echo "The necessary jar file is missing in the expected folder... Aborting"
	echo "Please generate the necessary jar file with dependencies as follows: "
	echo " cd ${basedir}"
	echo " mvn clean package"
	echo " cd bin"
fi

CLASS=it.cnr.isti.hpclab.ef.Generator
#INDEX_OPTS="-Dcollection.spec=$COLLECTION -Dterrier.index.path=$PATH -Dterrier.index.prefix=$PREFIX" 
CLASSPATH=$CLASSPATH:${basedir}"/target/terrier-eliasfano-indexer-2.0-jar-with-dependencies.jar"

exec "$JAVA" -cp $CLASSPATH $JAVA_HEAP_MAX $INDEX_OPTS $CLASS $PATH $PREFIX
