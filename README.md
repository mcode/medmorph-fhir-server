# MedMorph FHIR Server

This project is a FHIR server which is used for multiple actors (EHR and Knowledge Artifact Repository) in the [MedMorph Public Health Usecase](https://build.fhir.org/ig/HL7/fhir-medmorph/usecases.html). It is based on the [HAPI FHIR JPA Server (Version 4.1.0)](https://github.com/hapifhir/hapi-fhir-jpaserver-starter).

Note: Issues related to performance and Subscriptions were observed on HAPI 5.1.0

# Running Locally

The easiest way to run this server is to use docker.

```
./build-docker-image.sh
docker-compose up
```

This will create the `medmorph_fhir` image and spin up the `medmoprh_ehr` (on [http://localhost:8080/fhir](http://localhost:8080/fhir)) and `knowledge_artifact` (on [http://localhost:8090/fhir](http://localhost:8090/fhir)) containers.

Alternatively the image can be built and specific instances run. First, clone this repository. Then, from the repository root run:

```
docker build -t medmorph_fhir .
```

This will build the docker image for the reference server. Once the image has
been built, the server can be run with the following command:

```
docker run -p 8080:8080 medmorph_fhir -e AUTH_SERVER_ADDRESS=http://moonshot-dev.mitre.org:8090/auth/realms/ehr/protocol/openid-connect/ SERVER_ADDRESS=http://localhost:8080/fhir/
```

The server will then be browseable at
[http://localhost:8080](http://localhost:8080), and the
server's FHIR endpoint will be available at
[http://localhost:8080/fhir](http://localhost:8080/fhir)

# Configuration

Since this is a single repository and single docker image for multiple actors, each container should have its own properties. Right now the properties to configurate are `AUTH_SERVER_ADDRESS` and `SERVER_ADDRESS`. These are set through enviornment variables of the same names. Using compose will automatically create all necessary servers.

# Authorization

This server is protected by the SMART Backend Authentication protocol. The admin token is `admin`. The OAuth URLS can be found at the `/metadata` or `/.well-known/smart-configuration` endpoints.

Follow the [Register New Client](https://github.com/mcode/medmorph-fhir-server/wiki/Register-New-Client) wiki page to register a new client.

# Hosted Server

An instance of the EHR and Knowledge Artifact Repository is running on `pathways.mitre.org`.

| Service                      | Base URL                            |
| ---------------------------- | ----------------------------------- |
| EHR                          | http://pathways.mitre.org:8180/fhir |
| Knowldge Artifact Repository | http://pathways.mitre.org/8190/fhir |

## Steps to Update

The service will not automatically deploy when changes are made. To update the services:

```
[localhost] $ git push
[localhost] $ ssh pathways.mitre.org
[pathways.mitre.org] $ cd /opt/medmorph-fhir-server
[pathways.mitre.org] $ git pull
[pathways.mitre.org] $ docker-compose down
[pathways.mitre.org] $ ./build-docker-image.bat
[pathways.mitre.org] $ docker-compose up -d
```

Before running the last command you can optionally modify the urls and ports in the `docker-compose.yml` file.
