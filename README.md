# MedMorph Test EHR

This project is a test EHR FHIR server for the [MedMorph Public Health Usecase](https://build.fhir.org/ig/HL7/fhir-medmorph/usecases.html). It is based on the [HAPI FHIR JPA Server (Version 4.1.0)](https://github.com/hapifhir/hapi-fhir-jpaserver-starter).

Note: Issues related to performance and Subscriptions were observed on HAPI 5.1.0

# Running Locally

The easiest way to run this server is to use docker. First, clone this
repository. Then, from the repository root run:

```
docker build -t medmorph_ehr .
```

This will build the docker image for the reference server. Once the image has
been built, the server can be run with the following command:

```
docker run -p 8080:8080 medmorph_ehr
```

Alternatively the image can be built and run using

```
./build-docker-image.sh
docker-compose up
```

The server will then be browseable at
[http://localhost:8080](http://localhost:8080), and the
server's FHIR endpoint will be available at
[http://localhost:8080/fhir](http://localhost:8080/fhir)

# Authorization

This server is protected by the SMART Backend Authentication protocol. The admin token is `admin`. The OAuth URLS can be found at the `/metadata` or `/.well-known/smart-configuration` endpoints.

Follow the [Register New Client](https://github.com/mcode/medmorph-ehr/wiki/Register-New-Client) wiki page to register a new client.
