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
- `ImmuDB` : used to store metering information
- `RabbitMQ` : used to receive token invalidation info

  
  
The Gis Interface also connects with various DX dependencies namely
- Authorization Server : used to download the certificate for token decoding
- Catalogue Server : used to download the list of resources, access policies and query types supported on a resource.

----
## Setting up RabbitMQ for IUDX Gis Interface
- Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/databroker) to setup RMQ.


In order to connect to the appropriate RabbitMQ instance, required information such as dataBrokerIP, dataBrokerPort etc. should be updated in the DataBrokerVerticle module available in [config-example.json](configs/config-example.json).

**DataBrokerVerticle**
```
{
    id": "iudx.gis.server.databroker.DataBrokerVerticle",
    "verticleInstances": <num-of-verticle-instances>,
    "dataBrokerIP": "localhost",
    "dataBrokerPort": <port-number>,
    "dataBrokerVhost": <vHost-name>,
    "dataBrokerUserName": <username-for-rmq>,
    "dataBrokerPassword": <password-for-rmq>,
Adding default apiserver ports

- Default http port is 8080.
- Default port when ssl is enabled is 8443.
- Apiserver port configurable using parameter 'httpPort    "dataBrokerManagementPort": <management-port-number>,
    "connectionTimeout": <time-in-milliseconds>,
    "requestedHeartbeat": <time-in-seconds>,
    "handshakeTimeout": <time-in-milliseconds>,
    "requestedChannelMax": <num-of-max-channels>,
    "networkRecoveryInterval": <time-in-milliseconds>,
    "automaticRecoveryEnabled": "true"
}
```
---

## Setting up PostgreSQL for IUDX Gis Interface
-  Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/postgres) to setup PostgreSQL

**Note** : PostgresQL database should be configured with a RBAC user having CRUD privileges

In order to connect to the appropriate Postgres database, required information such as databaseIP, databasePort etc. should be updated in the PostgresVerticle module available in [config-example.json](configs/config-example.json).

**PostgresVerticle**
```
{
    "id": "iudx.gis.server.database.postgres.PostgresVerticle",
    "verticleInstances": <num-of-verticle-instances>,
    "databaseIp": "localhost",
    "databasePort": <port-number>,
    "databaseName": <database-name>,
    "databaseUserName": <username-for-psql>,
    "databasePassword": <password-for-psql>,
    "poolSize": <pool-size>
}
```
**DatabaseVerticle**
```
{
    "id": "iudx.gis.server.database.DatabaseVerticle",
    "verticleInstances": <num-of-verticle-instance>,
    "databaseIP": "localhost",
    "databasePort": <port-number>,
    "databaseName": <database-name>,
    "databaseUserName": <username-for-psql>,
    "databasePassword": <password-for-psql>,
    "dbClientPoolSize": <pool-size>
}
```


#### Schemas for PostgreSQL tables in IUDX Gis Interface
1. **Token Invalidation Table Schema**
```
CREATE TABLE IF NOT EXISTS revoked_tokens
(
   _id uuid NOT NULL,
   expiry timestamp with time zone NOT NULL,
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

## Setting up ImmuDB for IUDX Gis Interface
- Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/immudb) to setup ImmuDB.
- Refer [this](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/immudb/docker/immudb-config-generator/immudb-config-generator.py) to create table/user.
- In order to connect to the appropriate ImmuDB database, required information such as meteringDatabaseIP, meteringDatabasePort etc. should be updated in the MeteringVerticle module available in [config-example.json](configs/config-example.json).

**MeteringVerticle**

```
{
    "id": "iudx.gis.server.metering.MeteringVerticle",
    "verticleInstances": <num-of-verticle-instances>,
    "meteringDatabaseIP": "localhost",
    "meteringDatabasePort": <port-number>,
    "meteringDatabaseName": <database-name>,
    "meteringDatabaseUserName": <username-for-immudb>,
    "meteringDatabasePassword": <password-for-immudb>,
    "meteringDatabaseTableName": <table-name-for-immudb>
    "meteringPoolSize": <pool-size>
}
```

**Metering Table Schema**
```
CREATE TABLE IF NOT EXISTS gisaudit (
    id VARCHAR[128] NOT NULL, 
    api VARCHAR[128], 
    userid VARCHAR[128],
    epochtime INTEGER,
    resourceid VARCHAR[200],
    isotime VARCHAR[128],
    providerid VARCHAR[128],
    size INTEGER,
    PRIMARY KEY id
);
```

----

## Setting up RabbitMQ for IUDX Gis Interface
- Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/databroker) to setup RMQ.


In order to connect to the appropriate RabbitMQ instance, required information such as dataBrokerIP, dataBrokerPort etc. should be updated in the DataBrokerVerticle module available in [config-example.json](configs/config-example.json).

**DataBrokerVerticle**
```
{
    id": "iudx.gis.server.databroker.DataBrokerVerticle",
    "verticleInstances": <num-of-verticle-instances>,
    "dataBrokerIP": "localhost",
    "dataBrokerPort": <port-number>,
    "dataBrokerVhost": <vHost-name>,
    "dataBrokerUserName": <username-for-rmq>,
    "dataBrokerPassword": <password-for-rmq>,
    "dataBrokerManagementPort": <management-port-number>,
    "connectionTimeout": <time-in-milliseconds>,
    "requestedHeartbeat": <time-in-seconds>,
    "handshakeTimeout": <time-in-milliseconds>,
    "requestedChannelMax": <num-of-max-channels>,
    "networkRecoveryInterval": <time-in-milliseconds>,
    "automaticRecoveryEnabled": <true | false>
}
```

----
## Connecting with DX Catalogue Interface

In order to connect to the DX catalogue server, required information such as catServerHost, catServerPort etc. should be updated in the AuthenticationVerticle and ApiServerVerticle modules availabe in [config-example.json](configs/config-example.json).

**AuthenticationVerticle**
```
{
    "id": "iudx.gis.server.authenticator.AuthenticationVerticle",
    "verticleInstances": <number-of-verticle-instances,
    "audience": <gis-server-host>,
    "authServerHost": <auth-server-host>,
    "catServerHost": <catalogue-server-host>,
    "catServerPort": <catalogue-server-port>,
    "jwtIgnoreExpiry": <true | false>
}
```

**ApiServerVerticle**
```
{
    "id": "iudx.gis.server.apiserver.ApiServerVerticle",
    "ssl": <true | false>,
    "keystore": <path/to/keystore.jks>,
    "keystorePassword": <password-for-keystore>,
    "verticleInstances": <number-of-verticle-instances>,
    "catServerHost": <catalogue-server-host>,
    "catServerPort": <catalogue-server-port>,
    "httpPort" : <port/for/server>
}
```

## Connecting with DX Authorization Server

In order to connect to the DX authentication server, required information such as authServerHost should be updated in the AuthenticationVerticle module availabe in [config-example.json](configs/config-example.json).
```
{
   "id": "iudx.gis.server.authenticator.AuthenticationVerticle",
   "verticleInstances": <number-of-verticle-instances,
   "audience": <gis-server-host>,
   "authServerHost": <auth-server-host>,
   "catServerHost": <catalogue-server-host>,
   "catServerPort": <catalogue-server-port>,
   "jwtIgnoreExpiry": <true | false>
}
```
