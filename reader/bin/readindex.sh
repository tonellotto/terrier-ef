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
  echo "Usage: readindex COMMAND PATH PREFIX"
  echo "where"
  echo "  COMMAND 	is one among the following:"
  echo "     --printdocid	  prints the contents of the document index"
  echo "     --printlexicon   prints the contents of the lexicon"
  echo "     --printinverted  prints the contents of the inverted file"
  echo "     --printdirect	  prints the contents of the direct file"
  echo "     --printstats	  prints statistics about the indexed collection"
  echo "  PATH		is the resulting index path (must be absolute)"
  echo "  PREFIX  	is the resulting index prefix"
}

if [ $# = 0 ]; then
  print_usage
  exit
fi

if [ $1 == "-h" -o $1 == "-help"  -o $1 == "--help" -o $# -lt 3 ]; then
	print_usage
    exit
fi

COMMAND=$1
shift
PATH=$1
shift
PREFIX=$1
shift

if [ $COMMAND != "--printdocid" -a $COMMAND != "--printlexicon" -a $COMMAND != "--printinverted" -a $COMMAND != "--printdirect" -a $COMMAND != "--printstats" ]; then
	echo "Error: COMMAND ${COMMAND} is not expected... Aborting"
	exit
elif [[ ! -e $PATH ]]; then
	echo "Error: directory ${PATH} does not exist... Aborting"
	exit
elif [[ -f $PATH ]]; then
	echo "Error: directory ${PATH} is actually a file... Aborting"
	exit
elif [[ $PATH != /* ]]; then
	echo "Error: directory ${PATH} must be absolute... Aborting"
	exit
elif [[ ! -e ${PATH}"/"${PREFIX}".properties" ]]; then
	echo "Warning: directory ${PATH} seems to not contain an index with prefix ${PREFIX}... Aborting" 
	exit
fi

basedir=${bin%/*}
if [[ ! -e ${basedir}"/target/terrier-reader-2.0-jar-with-dependencies.jar" ]]; then
	echo "The necessary jar file is missing in the expected folder... Aborting"
	echo "Please generate the necessary jar file with dependencies as follows: "
	echo " cd ${basedir}"
	echo " mvn clean package"
	echo " cd bin"
fi

CLASS=it.cnr.isti.hpclab.Reading
INDEX_OPTS="-Dterrier.index.path=$PATH -Dterrier.index.prefix=$PREFIX" 
CLASSPATH=$CLASSPATH:${basedir}"/target/terrier-eliasfano-reader-2.0-jar-with-dependencies.jar"

#echo $CLASSPATH
exec "$JAVA" -cp $CLASSPATH $JAVA_HEAP_MAX $INDEX_OPTS $CLASS $COMMAND
