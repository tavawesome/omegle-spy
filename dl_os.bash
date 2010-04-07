#!/bin/bash

dir=omegle_spy
fname=OmegleSpy.jar

if [ -f $dir ]; then
	echo "$dir already exists as a file. Cannot continue"
	exit
else if [ ! -d $dir ]; then
	echo "Creating directory $dir ..."
	mkdir $dir
fi fi

cd $dir

echo "Downloading $fname ..."
if wget http://users.wpi.edu/~sfoley/OmegleSpy.jar -O $fname 2>/dev/null; then
	echo -n ""
else
	echo "Could not download $fname. Terminating"
	exit
fi

echo "Decompressing and recompiling source ..."
unzip $fname > /dev/null
rm -f $fname
make > /dev/null
mv $fname .$fname
rm -rf *
mv .$fname $fname

echo "Omegle Spy is ready to use?"
