#!/bin/bash

for project in {physhun,djvu,xalan,cloudstack}; do

	rm -rf ./toprocess/*
	mkdir ./toprocess/
	cp -r ~/mondo-codemodel/source-projects/$project ./toprocess/

	mkdir -p ~/mondo-codemodel/results/$project/

	for i in {1..5}; do
		~/4store-graph-driver/scripts/4s-restart.sh
		mongo jamopp --eval "db.dependencies.drop();"

		./transform-import-check.sh
		cp ./results/sum.txt ~/mondo-codemodel/results/$project/sum$i.txt
		echo $project $i done
	done

	mkdir -p ~/mondo-codemodel/exports/$project/
	cp -r ./export/ ~/mondo-codemodel/exports/$project/
done
