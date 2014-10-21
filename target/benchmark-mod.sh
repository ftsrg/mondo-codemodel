#!/bin/bash


for i in {1..5}; do
	rm -rf ./toprocess/*
	mkdir ./toprocess/
	cp -r ~/mondo-codemodel/source-projects/djvu ./toprocess/

	rm -rf export
	mkdir export
	rm -rf results
	mkdir results

	~/4store-graph-driver/scripts/4s-restart.sh
	mongo jamopp --eval "db.dependencies.drop();"


	java -jar ./jamoppdiscoverer-0.0.1-SNAPSHOT.jar | grep -e DepGraph -e ASG -e ImportTime | tee results/sum.txt
	./import-check.sh


	cp -r ~/mondo-codemodel/source-projects/djvu-mod/* ./toprocess/

	java -jar ./jamoppdiscoverer-0.0.1-SNAPSHOT.jar -modified ./toprocess/modified.txt | grep -e DepGraph -e ASG -e ImportTime | tee -a results/sum.txt
	./import-check.sh


	mkdir -p ~/mondo-codemodel/results/djvu-mod/
	cp ./results/sum.txt ~/mondo-codemodel/results/djvu-mod/sum$i.txt
	echo djvu-mod $i done
done

mkdir -p ~/mondo-codemodel/exports/djvu-mod/
cp -r ./export/ ~/mondo-codemodel/exports/djvu-mod/
