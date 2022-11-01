#!/bin/bash

#
# Copyright (c) 2022.
# This code is copyright Bernard Bou <1313ou@gmail.com>
# This software is released under the GNU Public License 3 <http://www.gnu.org/copyleft/gpl.html>.
#

a0=$1
if [ "${a0:0:2}" == "-D" ]; then
def=$1
shift
fi
#echo ${def}
#echo $@
java ${def} -jar sigma-core.jar $@
