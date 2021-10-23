# soen423_a3

##To generate proto files:

> protoc -I=. --java_out=. requestObject.proto

> protoc -I=. --java_out=. responseObject.proto

## Using WSIMPORT

> /usr/lib/jvm/jdk-10.0.2/bin/wsimport -keep src/com/roomreservation/roomreservation.wsdl