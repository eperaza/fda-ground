#!/bin/bash

if [ "$#" -lt "2" ]; then
  echo "Missing or invalid argument(s)"
  echo "Correct usage: ./Tests.sh [local|dev|test] [hello|tsp|config|preferences|properties|upload|listfrs|updatefrs|getfrsstatus|refresh|newuser|newusersa|registeruser|listusers|deleteuser|deleteusersa|getfp|getfpo|listsupa|getsupa] [additional-argument(s)]"
  exit 1
fi

USERNAME=araj@fdacustomertest.onmicrosoft.com
PASSWORD=Boeing12
TARGETHOST=localhost:8443
CERT=fdadvisor2_pkac_pair.p12
CERTPASS=boeing
if [ "$1" == "dev" ]; then
    TARGETHOST=fdagroundservices-test-ioscert.azurewebsites.net
elif [ "$1" == "test" ]; then
    TARGETHOST=fdagroundservices-test-stage.azurewebsites.net
elif [ "$1" == "prod" ]; then
    TARGETHOST=fdagroundservices-test.azurewebsites.net
    #TARGETHOST=fdagroundservices.azurewebsites.net
    #USERNAME=araj@flitedeckadvisor.com
    #CERT=fdadvisor_pkac_pair.p12
    #CERTPASS=Boeing12
elif [ "$1" == "local" ]; then
    TARGETHOST=localhost:8443
    #USERNAME=araj@flitedeckadvisor.com
    #CERT=fdadvisor_pkac_pair.p12
    #CERTPASS=Boeing12
else
  echo "Invalid host. Must be \"local\", \"dev\", \"test\" or \"prod\""
  exit 1 
fi

ACTION="$2"

echo "Getting access token (for user $USERNAME)..."
authResult=`curl -s -X POST "https://$TARGETHOST/login" --cert $CERT --pass $CERTPASS -H 'Cache-Control: no-cache' -H 'Content-Type: application/json' -d '{ "azUsername": "'"$USERNAME"'", "azPassword": "'"$PASSWORD"'" }' | jq -r '.authenticationResult'`
echo -------

accessToken=`echo $authResult | jq -r '.accessToken'`
refreshToken=`echo $authResult | jq -r '.refreshToken'`

if [ "$ACTION" == "hello" ]; then
  echo "Pinging..."
  curl -s "https://$TARGETHOST" --cert $CERT --pass $CERTPASS -H 'Cache-Control: no-cache' -H "Authorization: Bearer $accessToken"
  echo -e "\\nAccess token... $accessToken"
  echo -e -------
fi

if [ "$ACTION" == "tsp" ]; then
  echo "Fetching TSP file..."
  if [ "$#" -ne "3" ]; then
    echo "Missing or invalid argument(s)"
    echo "Correct usage: ./Tests.sh <host> tsp <tail-id> (e.g. n839ba, eidra, etc.)"
    exit 1
  fi
  TSPJSON="$3"
  curl -s "https://$TARGETHOST/download?file=$TSPJSON.json&type=tsp" --cert $CERT --pass $CERTPASS -H 'Cache-Control: no-cache' -H "Authorization: Bearer $accessToken"
  echo -e "\\n-------"
fi

if [ "$ACTION" == "config" ]; then
  echo "Fetching mobile configuration file..."
  if [ "$#" -ne "3" ]; then
    echo "Missing or invalid argument(s)"
    echo "Correct usage: ./Tests.sh <host> config <airline> (e.g. efo, ual, amx, etc.)"
    exit 1
  fi
  AIRLINE="$3"
  curl -s "https://$TARGETHOST/download?file=$AIRLINE.mobileconfig&type=config" --cert $CERT --pass $CERTPASS -H 'Cache-Control: no-cache' -H "Authorization: Bearer $accessToken"
  echo -e "\\n-------"
fi

if [ "$ACTION" == "preferences" ]; then
  echo "Fetching preferences file..."
  if [ "$#" -ne "3" ]; then
    echo "Missing or invalid argument(s)"
    echo "Correct usage: ./Tests.sh <host> preferences <airline> (e.g. efo, ual, amx, etc.)"
    exit 1
  fi
  AIRLINE="$3"
  curl -s "https://$TARGETHOST/download?file=$AIRLINE.plist&type=preferences" --cert $CERT --pass $CERTPASS -H 'Cache-Control: no-cache' -H "Authorization: Bearer $accessToken"
  echo -e "\\n-------"
fi

if [ "$ACTION" == "properties" ]; then
  echo "Fetching aircraft properties file..."
  if [ "$#" -ne "3" ]; then
    echo "Missing or invalid argument(s)"
    echo "Correct usage: ./Tests.sh <host> properties <airline> (e.g. xazam, eidra, etc.)"
    exit 1
  fi
  TAIL="$3"
  curl -s "https://$TARGETHOST/download?file=$TAIL.properties&type=properties" --cert $CERT --pass $CERTPASS -H 'Cache-Control: no-cache' -H "Authorization: Bearer $accessToken"
  echo -e \\n-------
fi

if [ "$ACTION" == "upload" ]; then
  echo "Uploading flight data file..."
  if [ "$#" -ne "3" ] || [ ! -f "$3" ]; then
    echo "Missing or invalid argument(s)"
    echo "Correct usage: ./Tests.sh <host> upload <full-path-to-flight-record-zip>"
    exit 1
  fi
  FLIGHTRECORD="$3"
  echo "$FLIGHTRECORD"
  curl -s -X POST "https://$TARGETHOST/uploadFlightRecord" --cert $CERT --pass $CERTPASS -H "Authorization: Bearer $accessToken" -H 'content-type: multipart/form-data' -F file=@"$FLIGHTRECORD"
  #curl -s -X POST "https://$TARGETHOST/uploadFile" --cert $CERT --pass $CERTPASS -H "Authorization: Bearer $accessToken" -H 'content-type: multipart/form-data' -F file=@"$FLIGHTRECORD"
  echo -e \\n-------
fi

if [ "$ACTION" == "listfrs" ]; then
  echo "List uploaded flight records..."
  curl -s "https://$TARGETHOST/listFlightRecords" --cert $CERT --pass $CERTPASS -H "Authorization: Bearer $accessToken"
  echo -e \\n-------
fi

if [ "$ACTION" == "updatefrs" ]; then
  echo "Update deleted-on-AID status of specified flight records..."
  curl -s "https://$TARGETHOST/updateFlightRecordStatusOnAid" --cert $CERT --pass $CERTPASS -H "Authorization: Bearer $accessToken" -H "Content-Type: application/json" -d '[ "FDA_ABCDEF_RDN631_LGAV_WSSL_20180601_182431Z_0045211.zip" ]'
  echo -e \\n-------
fi

if [ "$ACTION" == "getfrsstatus" ]; then
  echo "Getting status of specified flight records..."
  curl -s "https://$TARGETHOST/getStatusOfFlightRecords" --cert $CERT --pass $CERTPASS -H "Authorization: Bearer $accessToken" -H "Content-Type: application/json" -d '[ "UAL_N9988ba_UAL204_KORD_KBOS_20180602_182431Z_0045201.zip", "FDA_N5332A_FDA813_KGYY_KDAL_20180629_992701Z_007114.zip", "Hello_World" ]'
  echo -e \\n-------
fi


if [ "$ACTION" == "refresh" ]; then
  echo "Getting new refresh token..."
  newTokens=`curl -s -X POST "https://$TARGETHOST/refresh" --cert $CERT --pass $CERTPASS -H "cache-control: no-cache" -H 'content-type: application/json' -d '{ "refreshToken" : "'"$refreshToken"'" }'`
  accessToken=`echo $newTokens | jq -r '.accessToken'`
  refreshToken=`echo $newTokens | jq -r '.refreshToken'`

  echo "New access token = $accessToken"
  echo "New refresh token = $refreshToken"
  echo -e \\n-------
fi

if [ "$ACTION" == "newuser" ]; then
  if [ "$#" -ne "8" ]; then
    echo "Missing or invalid argument(s)"
    echo "Correct usage: ./Tests.sh <host> newuser <account-name> <first-name> <surname> <password> <work-email> <role>"
    exit 1
  fi
  echo "Creating new user..."
  NEWUSERNAME=$3
  NEWUSERFIRSTNAME=$4
  NEWUSERSURNAME=$5
  NEWUSERDISPLAYNAME="$4 $5"
  NEWUSERPASSWORD=$6
  NEWUSEREMAIL=$7
  NEWUSERROLE=$8
  
  curl -s -X POST "https://$TARGETHOST/airlinefocaladmin/users" --cert $CERT --pass $CERTPASS -H "Authorization: Bearer $accessToken" -H 'Content-Type: application/json' -d '{ "userPrincipalName": "'"$NEWUSERNAME"'", "givenName": "'"$NEWUSERFIRSTNAME"'", "surname": "'"$NEWUSERSURNAME"'", "displayName": "'"$NEWUSERDISPLAYNAME"'", "password": "'"$NEWUSERPASSWORD"'", "forceChangePasswordNextLogin": false, "otherMails": [ "'"$NEWUSEREMAIL"'" ], "roleGroupName": "role-'"$NEWUSERROLE"'" }'
fi

if [ "$ACTION" == "newusersa" ]; then
  if [ "$#" -ne "9" ]; then
    echo "Missing or invalid argument(s)"
    echo "Correct usage: ./Tests.sh <host> newusersa <account-name> <first-name> <surname> <password> <work-email> <airline> <role>"
    exit 1
  fi
  echo "Creating new user..."
  echo $accessToken
  NEWUSERNAME=$3
  NEWUSERFIRSTNAME=$4
  NEWUSERSURNAME=$5
  NEWUSERDISPLAYNAME="$4 $5"
  NEWUSERPASSWORD=$6
  NEWUSEREMAIL=$7
  NEWAIRLINE=$8
  NEWUSERROLE=$9
  
  curl -s -X POST "https://$TARGETHOST/superadmin/users" --cert $CERT --pass $CERTPASS -H "Authorization: Bearer $accessToken" -H 'Content-Type: application/json' -d '{ "userPrincipalName": "'"$NEWUSERNAME"'", "givenName": "'"$NEWUSERFIRSTNAME"'", "surname": "'"$NEWUSERSURNAME"'", "displayName": "'"$NEWUSERDISPLAYNAME"'", "password": "'"$NEWUSERPASSWORD"'", "forceChangePasswordNextLogin": false, "otherMails": [ "'"$NEWUSEREMAIL"'" ], "airlineGroupName": "airline-'"$NEWAIRLINE"'", "roleGroupName": "role-'"$NEWUSERROLE"'" }'
fi

if [ "$ACTION" == "registeruser" ]; then
  echo "Registering user..."
  if [ "$#" -ne "5" ]; then
    echo "Missing or invalid argument(s)"
    echo "Correct usage: ./Tests.sh <host> registeruser <registration-token> <account-name> <new-password>"
    exit 1
  fi
  REGISTRATIONTOKEN="$3"
  USERPRINCIPALNAME="$4"
  USERPASSWORD="$5"

  curl -s -X POST "https://$TARGETHOST/registeruser" --cert $CERT --pass $CERTPASS -H 'Content-Type: application/json' -d '{ "registrationToken": "'"$REGISTRATIONTOKEN"'", "username": "'"$USERPRINCIPALNAME"'", "password":"'"$USERPASSWORD"'" }'
fi

if [ "$ACTION" == "listusers" ]; then
  echo "Listing users..."

  curl -s "https://$TARGETHOST/airlinefocaladmin/users" --cert $CERT --pass $CERTPASS -H "Authorization: Bearer $accessToken" | jq
fi

if [ "$ACTION" == "deleteuser" ]; then
  echo "Deleting user..."
  if [ "$#" -ne "3" ]; then
    echo "Missing or invalid argument(s)"
    echo "Correct usage: ./Tests.sh <host> deleteuser <user-object-id>"
    exit 1
  fi
  USEROBJECTID="$3"

  curl -s -X DELETE "https://$TARGETHOST/airlinefocaladmin/users/$USEROBJECTID" --cert $CERT --pass $CERTPASS -H "Authorization: Bearer $accessToken"
fi

if [ "$ACTION" == "deleteusersa" ]; then
  echo "Deleting user... (superadmin)"
  if [ "$#" -ne "3" ]; then
    echo "Missing or invalid argument(s)"
    echo "Correct usage: ./Tests.sh <host> deleteusersa <user-object-id>"
    exit 1
  fi
  USEROBJECTID="$3"

  curl -s -X DELETE "https://$TARGETHOST/superadmin/users/$USEROBJECTID" --cert $CERT --pass $CERTPASS -H "Authorization: Bearer $accessToken"
fi

if [ "$ACTION" == "getfp" ]; then
  echo "Getting flight objects..."

  curl -s "https://$TARGETHOST/flight_objects" --cert $CERT --pass $CERTPASS -H "Authorization: Bearer $accessToken"
fi

if [ "$ACTION" == "getfpo" ]; then
  echo "Getting flight plan object..."
  if [ "$#" -ne "3" ]; then
    echo "Missing or invalid argument(s)"
    echo "Correct usage: ./Tests.sh <host> getfpo <flight-object-id>"
    exit 1
  fi
  FLIGHTOBJECTID="$3"

  curl -s "https://$TARGETHOST/flight_objects/$FLIGHTOBJECTID/show" --cert $CERT --pass $CERTPASS -H "Authorization: Bearer $accessToken"
fi

if [ "$ACTION" == "listsupa" ]; then
  echo "Getting SUPA releases..."
  if [ "$#" -ne "3" ]; then
    echo "Missing or invalid argument(s)"
    echo "Correct usage: ./Tests.sh <host> listsupa <number-of-versions>"
    exit 1
  fi
  VERSIONS="$3"

  curl -s "https://$TARGETHOST/supa-release-mgmt/list?versions=$VERSIONS" --cert $CERT --pass $CERTPASS -H "Authorization: Bearer $accessToken"
fi

if [ "$ACTION" == "getsupa" ]; then
  echo "Downloading SUPA release..."
  if [ "$#" -ne "3" ]; then
    echo "Missing or invalid argument(s)"
    echo "Correct usage: ./Tests.sh <host> getsupa <release-number> (e.g. 5.4.1, etc.)"
    exit 1
  fi
  RELEASE_VERSION="$3"
  echo $RELEASE_VERSION

  curl -s -OJ "https://$TARGETHOST/supa-release-mgmt/getRelease/$RELEASE_VERSION" --cert $CERT --pass $CERTPASS -H "Authorization: Bearer $accessToken"
fi

