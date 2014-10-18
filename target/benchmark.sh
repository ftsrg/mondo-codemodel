#!/bin/bash

for project in {physhun,djvu,xalan,cloudstack}; do

	rm -rf ./toprocess/*
	mkdir ./toprocess/
	cp ~/mondo-codemodel/source-projects/$project/ ./toprocess/

	mkdir -p ~/mondo-codemodel/results/$project/

	for i in {1..5}; do
		./transform-import-check.sh
		cp ./results/sum.txt ~/mondo-codemodel/results/$project/sum$i.txt
		echo $project $i done
	done

	mkdir -p ~/mondo-codemodel/exports/$project/
	cp ./export/ ~/mondo-codemodel/exports$/project/
done