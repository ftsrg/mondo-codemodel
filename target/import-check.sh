#!/bin/bash

# clean the database
~/4store-graph-driver/scripts/4s-boss-start-single.sh 
~/4store-graph-driver/scripts/4s-restart.sh

FILES=$(find `pwd`/export/ -name "*.ttl")


STIME=$(($(date +%s%N)/1000000))
for f in $FILES; do
#	STARTTIME=$(($(date +%s%N)/1000000))
	~/4store-graph-driver/scripts/4s-import.sh $f
#	let STARTTIME=$(($(date +%s%N)/1000000))-$STARTTIME
#	echo $f, $STARTTIME
done

let STIME=$(($(date +%s%N)/1000000))-$STIME
echo import time: $STIME >> results/sum.txt

STIME=$(($(date +%s%N)/1000000))
~/4store-graph-driver/scripts/4s-query.sh  "`cat ../queries/csmr-a.sparql`" | tail -n +2 | tee results/results-a.txt
let STIME=$(($(date +%s%N)/1000000))-$STIME
echo a runtime: $STIME >> results/sum.txt
cat results/results-a.txt | wc -l >> results/results-a.txt

STIME=$(($(date +%s%N)/1000000))
~/4store-graph-driver/scripts/4s-query.sh  "`cat ../queries/csmr-b.sparql`" | tail -n +2 | tee results/results-b.txt
let STIME=$(($(date +%s%N)/1000000))-$STIME
echo b runtime: $STIME >> results/sum.txt
cat results/results-b.txt | wc -l >> results/results-b.txt

STIME=$(($(date +%s%N)/1000000))
~/4store-graph-driver/scripts/4s-query.sh  "`cat ../queries/csmr-c.sparql`" | tail -n +2 | tee results/results-c.txt
let STIME=$(($(date +%s%N)/1000000))-$STIME
echo c runtime: $STIME >> results/sum.txt
cat results/results-c.txt | wc -l >> results/results-c.txt

STIME=$(($(date +%s%N)/1000000))
~/4store-graph-driver/scripts/4s-query.sh  "`cat ../queries/csmr-d.sparql`" | tail -n +2 | tee results/results-d.txt
let STIME=$(($(date +%s%N)/1000000))-$STIME
echo d runtime: $STIME >> results/sum.txt
cat results/results-d.txt | wc -l >> results/results-d.txt

#STIME=$(($(date +%s%N)/1000000))
#~/4store-graph-driver/scripts/4s-query.sh  "`cat ../queries/csmr-e.sparql`" | tail -n +2 | tee results/results-e.txt
#let STIME=$(($(date +%s%N)/1000000))-$STIME
#echo e runtime: $STIME 
#cat results/results-e.txt | wc -l >> results/results-e.txt

#STIME=$(($(date +%s%N)/1000000))
#~/4store-graph-driver/scripts/4s-query.sh  "`cat ../queries/csmr-f.sparql`" | tail -n +2 | tee results/results-f.txt
#let STIME=$(($(date +%s%N)/1000000))-$STIME
#echo f runtime: $STIME 
#cat results/results-f.txt | wc -l >> results/results-f.txt

cat results/results-a.txt >> results/sum.txt
cat results/results-b.txt >> results/sum.txt
cat results/results-c.txt >> results/sum.txt
cat results/results-d.txt >> results/sum.txt

