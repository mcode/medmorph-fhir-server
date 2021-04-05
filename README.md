# MedMorph FHIR Server

This project is a FHIR server which is used for multiple actors (EHR and Knowledge Artifact Repository) in the [MedMorph Public Health Usecase](https://build.fhir.org/ig/HL7/fhir-medmorph/usecases.html). It is based on the [HAPI FHIR JPA Server (Version 4.1.0)](https://github.com/hapifhir/hapi-fhir-jpaserver-starter).

Note: Issues related to performance and Subscriptions were observed on HAPI 5.1.0

# Running Locally

The easiest way to run this server is to use docker.  If using docker-for-windows or docker-for-mac, make sure the docker machine is allocated enough memory to run all three FHIR servers.  The three containers together require approximately 2.5GB of memory.

By default, docker on windows/mac will only allocate 2GB of local memory for docker containers.  To change this, use the Docker GUI, accessed through the docker toolbar icon or app, and navigate to preferences.  Open the resources/advanced tab and increase the memory from 2GB to at least 3GB.  This should provide the fhir servers with enough memory to run properly.

Then, run the following commands:

```
./build-docker-image.bat
docker-compose up
```

This will create the `medmorph_fhir` image and spin up the `medmoprh_ehr` (on [http://localhost:8180/fhir](http://localhost:8180/fhir)), `knowledge_artifact` (on [http://localhost:8190/fhir](http://localhost:8190/fhir)), and `public_health_authority` (on [http://localhost:8181/fhir](http://localhost:8181/fhir)) containers.

Alternatively the image can be built and specific instances run. First, clone this repository. Then, from the repository root run:

```
docker build -t medmorph_fhir .
```

This will build the docker image for the reference server. Once the image has
been built, the server can be run with the following command:

```
docker run -p 8080:8080 -e AUTH_SERVER_ADDRESS=http://moonshot-dev.mitre.org:8090/auth/realms/ehr/protocol/openid-connect/ -e SERVER_ADDRESS=http://localhost:8080/fhir/ -e SERVER_TITLE=EHR medmorph_fhir
```

The server will then be browseable at
[http://localhost:8080](http://localhost:8080), and the
server's FHIR endpoint will be available at
[http://localhost:8080/fhir](http://localhost:8080/fhir)

# Configuration

Since this is a single repository and single docker image for multiple actors, each container should have its own properties. Right now the properties to configurate are `AUTH_SERVER_ADDRESS`, `SERVER_ADDRESS`, `SERVER_TITLE`, and `ADMIN_TOKEN`. These are set through enviornment variables of the same names. Using compose will automatically create all necessary servers.

# Authorization

This server is protected by the SMART Backend Authentication protocol. The admin token is `admin`. The OAuth URLS can be found at the `/metadata` or `/.well-known/smart-configuration` endpoints.

Follow the [Register New Client](https://github.com/mcode/medmorph-fhir-server/wiki/Register-New-Client) wiki page to register a new client.

# License

Copyright 2021 The MITRE Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
