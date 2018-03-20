# Setup as a Docker container

If Docker is installed on your computer, you can setup the application by building a Docker image and provisioning a container from that image.

> SUPA and the Demo application can also be included by copying corresponding WAR files along with the application's WAR file.

The following steps need to be performed before building the Docker image:
1. Build application and copy WAR file into **dist/** sub-folder, named as _mobilebackendapi.war_.
	> Copy WAR files for SUPA (_supa.war_) and Demo (_demo.war_), as required, into same sub-folder.
2. Copy Demo flights into **flightdata/** sub-folder.
	> A Demo flight is represented by a folder containing a binary file containing flight data and an XML file representing the flight plan.
3. Request to obtain certificate and private key files related to the two client certificates from the Git repo maintainer, and copy all the files into the **vault/** sub-folder. _These are not stored on Git for obvious reasons!_

To build the Docker image, create a container (based on the image) and start it, run the following command from a terminal window:
```
docker-compose up
```

The process will take a while to build the Docker image, followed by a relatively quick startup of the container. If all goes well, the Tomcat server log will display on the terminal.

To log into the running container, run the following command from another terminal window:
```
docker exec -it fda_supa bash
```
This command will log you into the running container, straight into the _/usr/local/tomcat/_ folder.