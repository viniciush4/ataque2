#!/bin/bash

echo ""
echo "Informe: <HOST> <NOME_ESCRAVO> <MODO_OVERHEAD? 0 / 1>"
read HOST NOME OVERHEAD 
cd ./bin
java -cp .:../glassfish5/glassfish/lib/gf-client.jar br/inf/ufes/escravo/SlaveImpl "$HOST" "$NOME" "$OVERHEAD" 



