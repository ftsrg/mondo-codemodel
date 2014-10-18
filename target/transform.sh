#!/bin/bash

rm -rf export
mkdir export
rm -rf results
mkdir results

mongo jamopp --eval "db.dependencies.drop();"

java -jar ./jamoppdiscoverer-0.0.1-SNAPSHOT.jar | grep -e DepGraph -e ASG | tee results/sum.txt
