SETUP GUIDE
----

This document contains the installation and configuration processes
of the external modules of each Verticle in IUDX Gis Interface.

<p align="center">
<img src="./docs/gis_server_overview.png">
</p>

The Gis Interface connects with various external dependencies namely
- `PostgreSQL` :  used to store and query data related to
  - Token Invalidation
  - Fetching data
- `RabbitMQ` : used to receive token invalidation info

  
  
The Gis Interface also connects with various DX dependencies namely
- Authorization Server : used to download the certificate for token decoding
- Catalogue Server : used to download the list of resources, access policies and query types supported on a resource.
- Auditing Server : used to store information of metering in ImmuDB and Postgres.

## Create configs files

Make a copy of sample configs directory and add appropriate values to all files.

```console
 cp -r example-configs/* .
```

```
# configs directory after generation of configs files
configs/
├── config-dev.json
└── config-test.json
├── keystore.jks
```

----

## Setting up PostgreSQL for IUDX Gis Interface
-  Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/postgres) to setup PostgreSQL

**Note** : PostgresQL database should be configured with a RBAC user having CRUD privileges

In order to connect to the appropriate Postgres database, required information such as databaseIP, databasePort etc. should be updated in the PostgresVerticle module available in [config-example.json](example-configs/config-dev.json).

**PostgresVerticle**
```
{
    "id": "iudx.gis.server.database.postgres.PostgresVerticle",
    "isWorkerVerticle": <true/false>,
    "verticleInstances": <num-of-verticle-instances>,
    "databaseIp": "localhost",
    "databasePort": <port-number>,
    "databaseName": <database-name>,
    "databaseUserName": <username-for-psql>,
    "databasePassword": <password-for-psql>,
    "poolSize": <pool-size>
}

```
#### Schemas for PostgreSQL tables in IUDX Gis Interface
1. **Token Invalidation Table Schema**
```
CREATE TABLE IF NOT EXISTS revoked_tokens
(
   _id uuid NOT NULL,
   expiry timestamp without time zone NOT NULL,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL,
   CONSTRAINT revoke_tokens_pk PRIMARY KEY (_id)
);
```

2. **Gis Table Schema**
```
CREATE TABLE IF NOT EXISTS gis
(
   iudx_resource_id character varying NOT NULL,
   url varchar NOT NULL,
   isOpen BOOLEAN NOT NULL,
   port integer NOT NULL,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL,
   username varchar,
   password varchar,
   tokenurl character varying,
   CONSTRAINT gis_pk PRIMARY KEY (iudx_resource_id)
);
```
----

## Setting up Metering for IUDX Gis Interface
- In Metering Verticle we are pushing data in Auditing Server through RabbitMQ. [config-example.json](example-configs/config-dev.json).
- RabbitMQ is used to create an auditing exchange under the IUDX-Internal vhosts.

**MeteringVerticle**

```
{
    "id": "iudx.gis.server.metering.MeteringVerticle",
    "verticleInstances": <num-of-verticle-instances>,
}
```

----

## Setting up Internal Cache Store
- In order to connect with the internal cache, we need to add cache verticle configs available in [config-example.json](example-configs/config-dev.json).

**CacheVerticle**

```
{
  "id": "iudx.gis.server.cache.CacheVerticle",
  "verticleInstances": 1
}
```

----

## Setting up RabbitMQ for IUDX Gis Interface
- Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/databroker) to setup RMQ.
- Refer the table for the permission for RMQ setup.

```
| Permission | Exchange | RoutingKey | Type of Exchange | VHosts        |
|------------|----------|------------|------------------|---------------|
| Read/Write | auditing | #          | Direct           | IUDX-INTERNAL |
```

In order to connect to the appropriate RabbitMQ instance, required information such as dataBrokerIP, dataBrokerPort etc. should be updated in the DataBrokerVerticle module available in [config-example.json](example-configs/config-dev.json).

**DataBrokerVerticle**
```
{
    id": "iudx.gis.server.databroker.DataBrokerVerticle",
    "verticleInstances": <num-of-verticle-instances>,
    "dataBrokerIP": "localhost",
    "dataBrokerPort": <port-number>,
    "dataBrokerUserName": <username-for-rmq>,
    "dataBrokerPassword": <password-for-rmq>,
    "dataBrokerManagementPort": <port-number>,
    "connectionTimeout": <time-in-milliseconds>,
    "requestedHeartbeat": <time-in-seconds>,
    "handshakeTimeout": <time-in-milliseconds>,
    "requestedChannelMax": <num-of-max-channels>,
    "networkRecoveryInterval": <time-in-milliseconds>,
    "automaticRecoveryEnabled": <true | false>,
    "prodVhost": <prodVhost>,
    "internalVhost": <internalVhost>,
    "externalVhost": <externalVhost>
    
}
```

----
## Connecting with DX Catalogue Interface

In order to connect to the DX catalogue server, required information such as catServerHost, catServerPort etc. should be updated in the AuthenticationVerticle and ApiServerVerticle modules availabe in [config-example.json](example-configs/config-dev.json).

**AuthenticationVerticle**
```
{
    "id": "iudx.gis.server.authenticator.AuthenticationVerticle",
    "verticleInstances": <number-of-verticle-instances>,
    "audience": <gis-server-host>,
    "authServerHost": <auth-server-host>,
    "catServerHost": <catalogue-server-host>,
    "catServerPort": <catalogue-server-port>,
    "jwtIgnoreExpiry": <true | false>,
    "iss": "<iss-value>"
}
```

**ApiServerVerticle**
```
{
    "id": "iudx.gis.server.apiserver.ApiServerVerticle",
    "ssl": <true | false>,
    "production": <true | false>,
    "keystore": <path/to/keystore.jks>,
    "keystorePassword": <password-for-keystore>,
    "verticleInstances": <number-of-verticle-instances>,
    "catServerHost": <catalogue-server-host>,
    "catServerPort": <catalogue-server-port>,
    "httpPort" : <port/for/server>
}
```

## Connecting with DX Authorization Server

In order to connect to the DX authentication server, required information such as authServerHost should be updated in the AuthenticationVerticle module availabe in [config-example.json](example-configs/config-dev.json).
```
{
   "id": "iudx.gis.server.authenticator.AuthenticationVerticle",
   "verticleInstances": <number-of-verticle-instances>,
   "audience": <gis-server-host>,
   "authServerHost": <auth-server-host>,
   "catServerHost": <catalogue-server-host>,
   "catServerPort": <catalogue-server-port>,
   "jwtIgnoreExpiry": <true | false>,
   "iss": "<iss-value>"
}
```
