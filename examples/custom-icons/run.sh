#!/bin/bash
set -e
MSC_HOME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )/../.."
SEP=":"
if [ "$(expr substr $(uname -s) 1 9)" == "CYGWIN_NT" ]; then
    MSC_HOME=`cygpath -w $MSC_HOME`
    SEP=";"
fi
THIS_DIR=$MSC_HOME/examples/custom-icons
echo $MSC_HOME/bin/mscviewer -r $THIS_DIR/food$SEP$THIS_DIR/animals $THIS_DIR/test.msc

$MSC_HOME/bin/mscviewer -r $THIS_DIR/food$SEP$THIS_DIR/animals $THIS_DIR/test.msc
