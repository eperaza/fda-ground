#!/bin/bash

echo "Getting access token (for user test1@fdacustomertest.onmicrosoft.com)..."
authResult=`curl -s -X POST https://localhost/login --cert fda-ground-kv-client1-20180314.pfx --pass "" -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' -d '{ "azUsername": "test1@fdacustomertest.onmicrosoft.com", "azPassword": "Boeing12" }' | jq -r '.authenticationResult'`
echo -------

accessToken=`echo $authResult | jq -r '.accessToken'`
refreshToken=`echo $authResult | jq -r '.refreshToken'`

echo "Fetching TSP file..."
curl -s 'https://localhost/download?file=edira.json&type=tsp' --cert fda-ground-kv-client2-20180314.pfx --pass "" -H 'Cache-Control: no-cache' -H 'Authorization: Bearer $accessToken'
echo -------

echo "Fetching properties file..."
curl -s 'https://localhost/download?file=edira.properties&type=properties' --cert fda-ground-kv-client2-20180314.pfx --pass "" -H 'Cache-Control: no-cache' -H 'Authorization: Bearer $accessToken'
echo -------

echo "Getting new refresh token..."
curl -s -X POST https://localhost/refresh --cert fda-ground-kv-client1-20180314.pfx --pass "" -H 'cache-control: no-cache' -H 'content-type: application/json' -d '{ "refreshToken" : "'$refreshToken'" }' | jq -r '.refresh_token'

