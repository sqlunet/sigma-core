#!/bin/bash

a0=$1
if [ "${a0:0:2}" == "-D" ]; then
def=$1
shift
fi
#echo ${def}
#echo $@
java ${def} -jar sigma-core.jar $@
