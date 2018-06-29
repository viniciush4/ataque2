#!/bin/bash

echo ""
echo "Informe: <HOST> <M>"
read HOST M
cd ./bin
java -cp .:../glassfish5/glassfish/lib/gf-client.jar br/inf/ufes/mestre/MasterImpl "$HOST" "$M"
