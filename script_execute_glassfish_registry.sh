#!/bin/bash

cd bin
echo "Iniciando Registry.."
rmiregistry &
echo "Registry online. Iniciando GlassFish.."
cd ..
cd ./glassfish5/bin
./asadmin start-domain
./asadmin add-resources glassfish-queues.xml
echo "GlassFich Online e Filas de SubAtks e Guesses carregadas." 




