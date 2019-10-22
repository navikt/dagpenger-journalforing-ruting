#!/usr/bin/env bash

if test -f /secrets/serviceuser/srvdagpenger-journ/username;
then
    export  SRVDAGPENGER_JOURNALFORING_RUTING_USERNAME=$(cat /secrets/serviceuser/srvdagpenger-journ/username)
fi
if test -f /secrets/serviceuser/srvdagpenger-journ/password;
then
    export  SRVDAGPENGER_JOURNALFORING_RUTING_PASSWORD=$(cat /secrets/serviceuser/srvdagpenger-journ/password)
fi


