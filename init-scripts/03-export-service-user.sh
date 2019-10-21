#!/usr/bin/env bash

if test -f /secret/serviceuser/srvdagpenger-journ/username;
then
    export  SRVDAGPENGER_JOURNALFORING_RUTING_USERNAME=$(cat /secret/serviceuser/srvdagpenger-journ/username)
fi

if test -f /secret/serviceuserver/srvdagpenger-journ/password;
then
    export  SRVDAGPENGER_JOURNALFORING_RUTING_PASSWORD=$(cat /secret/serviceuser/srvdagpenger-journ/password)
fi


