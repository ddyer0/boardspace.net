#!/bin/sh
cd $HOME/src
sort -n -r -t' ' +2 $HOME/ratings/pett/results-rated > best.1
./dobest > $HOME/src/data/help/arden
echo >> $HOME/src/data/help/arden
./genstat >> $HOME/src/data/help/arden
./genstat > $HOME/src/data/help/opercents
echo "Ratings distribution based on number of rated games" > $HOME/src/data/help/ogpercents
./gpercent >> $HOME/src/data/help/ogpercents
#average = sum of all/number of players
#arden [99]: no math formula, just position in a sorted list (for median)
#arden [99]: percentiles are just calculated by adding all the players ranked below you divided by total

#sum of (each rating)SQUARED/number number in list (i.e. 226 currently)
#then take the square root of that (for stddev)

