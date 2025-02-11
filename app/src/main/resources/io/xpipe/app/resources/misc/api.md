---
title: XPipe API Documentation v14.0
language_tabs:
  - javascript: JavaScript
  - python: Python
  - java: Java
  - go: Go
  - shell: Shell
language_clients:
  - javascript: ""
  - python: ""
  - java: ""
  - go: ""
  - shell: ""
toc_footers:
  - <a href="https://xpipe.io/pricing">XPipe - Plans and pricing</a>
includes: []
search: true
highlight_theme: darkula
headingLevel: 2

---

<h1 id="xpipe-api-documentation">XPipe API Documentation v14.0</h1>

The XPipe API provides programmatic access to XPipe’s features.
You can get started by either using this page as an API reference or alternatively import the OpenAPI definition file into your API client of choice:

<a download href="/openapi.yaml" style="font-size: 20px">OpenAPI .yaml specification</a>

The XPipe application will start up an HTTP server that can be used to send requests.
Note that this server is HTTP-only for now as it runs only on localhost. HTTPS requests are not accepted.

You can either call the API directly or using the official [XPipe Python API](https://github.com/xpipe-io/xpipe-python-api).

To start off with the API, you can query connections based on various filters.
With the matched connections, you can start remote shell sessions for each one and run arbitrary commands in them.
You get the command exit code and output as a response, allowing you to adapt your control flow based on command outputs.
Any kind of passwords and other secrets are automatically provided by XPipe when establishing a shell connection.
If a required password is not stored and is set to be dynamically prompted, the running XPipe application will ask you to enter any required passwords.

See the authentication handshake below on how to authenticate prior to sending requests.
For development, you can also skip the authentication step by disabling it in the settings menu.

Base URLs:

* <a href="http://localhost:21721">http://localhost:21721</a>

Table of contents:
[TOC]

# Authentication

- HTTP Authentication, scheme: bearer The bearer token used is the session token that you receive from the handshake exchange.

<h1 id="xpipe-api-documentation-default">Default</h1>

## Establish a new API session

<a id="opIdhandshake"></a>

`POST /handshake`

Prior to sending requests to the API, you first have to establish a new API session via the handshake endpoint.
In the response you will receive a session token that you can use to authenticate during this session.

This is done so that the daemon knows what kind of clients are connected and can manage individual capabilities for clients.
If your client is running on the same system as the daemon, you can choose the local authentication method to avoid having to deal with API keys.
If your client does not have file system access, e.g. if it is running remotely, then you have to use an API key.

Note that for development you can also turn off the required authentication in the XPipe settings menu, allowing you to send unauthenticated requests.

> Body parameter

```json
{
  "auth": {
    "type": "ApiKey",
    "key": "<API key>"
  },
  "client": {
    "type": "Api",
    "name": "My client name"
  }
}
```

<h3 id="establish-a-new-api-session-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[HandshakeRequest](#schemahandshakerequest)|true|none|

> Example responses

> 200 Response

```json
{
  "sessionToken": "string"
}
```

<h3 id="establish-a-new-api-session-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The handshake was successful. The returned token can be used for authentication in this session. The token is valid as long as XPipe is running.|[HandshakeResponse](#schemahandshakeresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|[ClientErrorResponse](#schemaclienterrorresponse)|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|[ServerErrorResponse](#schemaservererrorresponse)|

<aside class="success">
This operation does not require authentication
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "auth": {
    "type": "ApiKey",
    "key": "<API key>"
  },
  "client": {
    "type": "Api",
    "name": "My client name"
  }
}';
const headers = {
  'Content-Type':'application/json',
  'Accept':'application/json'
};

fetch('http://localhost:21721/handshake',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```python
import requests
headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
}

data = """
{
  "auth": {
    "type": "ApiKey",
    "key": "<API key>"
  },
  "client": {
    "type": "Api",
    "name": "My client name"
  }
}
"""
r = requests.post('http://localhost:21721/handshake', headers = headers, data = data)

print(r.json())

```

```java
var uri = URI.create("http://localhost:21721/handshake");
var client = HttpClient.newHttpClient();
var request = HttpRequest
        .newBuilder()
        .uri(uri)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("""
{
  "auth": {
    "type": "ApiKey",
    "key": "<API key>"
  },
  "client": {
    "type": "Api",
    "name": "My client name"
  }
}
        """))
        .build();
var response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Content-Type": []string{"application/json"},
        "Accept": []string{"application/json"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "http://localhost:21721/handshake", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

```shell
# You can also use wget
curl -X POST http://localhost:21721/handshake \
  -H 'Content-Type: application/json' \  -H 'Accept: application/json' \
  --data '
{
  "auth": {
    "type": "ApiKey",
    "key": "<API key>"
  },
  "client": {
    "type": "Api",
    "name": "My client name"
  }
}
'

```

</details>

## Query connections

<a id="opIdconnectionQuery"></a>

`POST /connection/query`

Queries all connections using various filters.

The filters support globs and can match the category names and connection names.
All matching is case insensitive.

> Body parameter

```json
{
  "categoryFilter": "*",
  "connectionFilter": "*",
  "typeFilter": "*"
}
```

<h3 id="query-connections-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[ConnectionQueryRequest](#schemaconnectionqueryrequest)|true|none|

> Example responses

> The query was successful. The body contains all matched connections.

```json
{
  "found": [
    "f0ec68aa-63f5-405c-b178-9a4454556d6b"
  ]
}
```

> 400 Response

```json
{
  "message": "string"
}
```

<h3 id="query-connections-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The query was successful. The body contains all matched connections.|[ConnectionQueryResponse](#schemaconnectionqueryresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|[ClientErrorResponse](#schemaclienterrorresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|[ServerErrorResponse](#schemaservererrorresponse)|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "categoryFilter": "*",
  "connectionFilter": "*",
  "typeFilter": "*"
}';
const headers = {
  'Content-Type':'application/json',
  'Accept':'application/json',
  'Authorization':'Bearer {access-token}'
};

fetch('http://localhost:21721/connection/query',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```python
import requests
headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json',
  'Authorization': 'Bearer {access-token}'
}

data = """
{
  "categoryFilter": "*",
  "connectionFilter": "*",
  "typeFilter": "*"
}
"""
r = requests.post('http://localhost:21721/connection/query', headers = headers, data = data)

print(r.json())

```

```java
var uri = URI.create("http://localhost:21721/connection/query");
var client = HttpClient.newHttpClient();
var request = HttpRequest
        .newBuilder()
        .uri(uri)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
{
  "categoryFilter": "*",
  "connectionFilter": "*",
  "typeFilter": "*"
}
        """))
        .build();
var response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Content-Type": []string{"application/json"},
        "Accept": []string{"application/json"},
        "Authorization": []string{"Bearer {access-token}"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "http://localhost:21721/connection/query", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

```shell
# You can also use wget
curl -X POST http://localhost:21721/connection/query \
  -H 'Content-Type: application/json' \  -H 'Accept: application/json' \  -H 'Authorization: Bearer {access-token}' \
  --data '
{
  "categoryFilter": "*",
  "connectionFilter": "*",
  "typeFilter": "*"
}
'

```

</details>

## Connection information

<a id="opIdconnectionInfo"></a>

`POST /connection/info`

Queries detailed information about a connection.

> Body parameter

```json
{
  "connections": [
    "f0ec68aa-63f5-405c-b178-9a4454556d6b"
  ]
}
```

<h3 id="connection-information-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[ConnectionInfoRequest](#schemaconnectioninforequest)|true|none|

> Example responses

> The query was successful. The body contains the detailed connection information.

```json
{
  "infos": [
    {
      "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
      "category": [
        "default"
      ],
      "name": [
        "local machine"
      ],
      "type": "local",
      "rawData": {},
      "usageCategory": "shell",
      "lastUsed": "2024-05-31T11:53:02.408504600Z",
      "lastModified": "2024-06-23T21:15:25.608097Z",
      "state": {}
    }
  ]
}
```

> 400 Response

```json
{
  "message": "string"
}
```

<h3 id="connection-information-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The query was successful. The body contains the detailed connection information.|[ConnectionInfoResponse](#schemaconnectioninforesponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|[ClientErrorResponse](#schemaclienterrorresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|[ServerErrorResponse](#schemaservererrorresponse)|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "connections": [
    "f0ec68aa-63f5-405c-b178-9a4454556d6b"
  ]
}';
const headers = {
  'Content-Type':'application/json',
  'Accept':'application/json',
  'Authorization':'Bearer {access-token}'
};

fetch('http://localhost:21721/connection/info',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```python
import requests
headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json',
  'Authorization': 'Bearer {access-token}'
}

data = """
{
  "connections": [
    "f0ec68aa-63f5-405c-b178-9a4454556d6b"
  ]
}
"""
r = requests.post('http://localhost:21721/connection/info', headers = headers, data = data)

print(r.json())

```

```java
var uri = URI.create("http://localhost:21721/connection/info");
var client = HttpClient.newHttpClient();
var request = HttpRequest
        .newBuilder()
        .uri(uri)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
{
  "connections": [
    "f0ec68aa-63f5-405c-b178-9a4454556d6b"
  ]
}
        """))
        .build();
var response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Content-Type": []string{"application/json"},
        "Accept": []string{"application/json"},
        "Authorization": []string{"Bearer {access-token}"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "http://localhost:21721/connection/info", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

```shell
# You can also use wget
curl -X POST http://localhost:21721/connection/info \
  -H 'Content-Type: application/json' \  -H 'Accept: application/json' \  -H 'Authorization: Bearer {access-token}' \
  --data '
{
  "connections": [
    "f0ec68aa-63f5-405c-b178-9a4454556d6b"
  ]
}
'

```

</details>

## Add new connection

<a id="opIdconnectionAdd"></a>

`POST /connection/add`

Creates the new connection in the xpipe vault from raw json data.
This can also perform an optional validation first to make sure that the connection can be established.

If an equivalent connection already exists, no new one will be added.

> Body parameter

```json
{
  "name": "my connection",
  "validate": true,
  "category": "97458c07-75c0-4f9d-a06e-92d8cdf67c40",
  "data": {
    "type": "shellEnvironment",
    "commands": null,
    "host": {
      "storeId": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
    },
    "shell": "pwsh",
    "elevated": false
  }
}
```

<h3 id="add-new-connection-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[ConnectionAddRequest](#schemaconnectionaddrequest)|true|none|

> Example responses

> The request was successful. The connection was added.

```json
{
  "connection": "36ad9716-a209-4f7f-9814-078d3349280c"
}
```

> 400 Response

```json
{
  "message": "string"
}
```

<h3 id="add-new-connection-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The request was successful. The connection was added.|[ConnectionAddResponse](#schemaconnectionaddresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|[ClientErrorResponse](#schemaclienterrorresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|[ServerErrorResponse](#schemaservererrorresponse)|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "name": "my connection",
  "validate": true,
  "category": "97458c07-75c0-4f9d-a06e-92d8cdf67c40",
  "data": {
    "type": "shellEnvironment",
    "commands": null,
    "host": {
      "storeId": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
    },
    "shell": "pwsh",
    "elevated": false
  }
}';
const headers = {
  'Content-Type':'application/json',
  'Accept':'application/json',
  'Authorization':'Bearer {access-token}'
};

fetch('http://localhost:21721/connection/add',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```python
import requests
headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json',
  'Authorization': 'Bearer {access-token}'
}

data = """
{
  "name": "my connection",
  "validate": true,
  "category": "97458c07-75c0-4f9d-a06e-92d8cdf67c40",
  "data": {
    "type": "shellEnvironment",
    "commands": null,
    "host": {
      "storeId": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
    },
    "shell": "pwsh",
    "elevated": false
  }
}
"""
r = requests.post('http://localhost:21721/connection/add', headers = headers, data = data)

print(r.json())

```

```java
var uri = URI.create("http://localhost:21721/connection/add");
var client = HttpClient.newHttpClient();
var request = HttpRequest
        .newBuilder()
        .uri(uri)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
{
  "name": "my connection",
  "validate": true,
  "category": "97458c07-75c0-4f9d-a06e-92d8cdf67c40",
  "data": {
    "type": "shellEnvironment",
    "commands": null,
    "host": {
      "storeId": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
    },
    "shell": "pwsh",
    "elevated": false
  }
}
        """))
        .build();
var response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Content-Type": []string{"application/json"},
        "Accept": []string{"application/json"},
        "Authorization": []string{"Bearer {access-token}"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "http://localhost:21721/connection/add", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

```shell
# You can also use wget
curl -X POST http://localhost:21721/connection/add \
  -H 'Content-Type: application/json' \  -H 'Accept: application/json' \  -H 'Authorization: Bearer {access-token}' \
  --data '
{
  "name": "my connection",
  "validate": true,
  "category": "97458c07-75c0-4f9d-a06e-92d8cdf67c40",
  "data": {
    "type": "shellEnvironment",
    "commands": null,
    "host": {
      "storeId": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
    },
    "shell": "pwsh",
    "elevated": false
  }
}
'

```

</details>

## Add new category

<a id="opIdcategoryAd"></a>

`POST /category/add`

Creates a new empty category in the vault.

New categories always need a parent as it's not allowed to create root categories.

> Body parameter

```json
{
  "name": "my category",
  "parent": "97458c07-75c0-4f9d-a06e-92d8cdf67c40"
}
```

<h3 id="add-new-category-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[CategoryAddRequest](#schemacategoryaddrequest)|true|none|

> Example responses

> The request was successful. The category was added.

```json
{
  "category": "36ad9716-a209-4f7f-9814-078d3349280c"
}
```

> 400 Response

```json
{
  "message": "string"
}
```

<h3 id="add-new-category-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The request was successful. The category was added.|[CategoryAddResponse](#schemacategoryaddresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|[ClientErrorResponse](#schemaclienterrorresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|[ServerErrorResponse](#schemaservererrorresponse)|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "name": "my category",
  "parent": "97458c07-75c0-4f9d-a06e-92d8cdf67c40"
}';
const headers = {
  'Content-Type':'application/json',
  'Accept':'application/json',
  'Authorization':'Bearer {access-token}'
};

fetch('http://localhost:21721/category/add',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```python
import requests
headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json',
  'Authorization': 'Bearer {access-token}'
}

data = """
{
  "name": "my category",
  "parent": "97458c07-75c0-4f9d-a06e-92d8cdf67c40"
}
"""
r = requests.post('http://localhost:21721/category/add', headers = headers, data = data)

print(r.json())

```

```java
var uri = URI.create("http://localhost:21721/category/add");
var client = HttpClient.newHttpClient();
var request = HttpRequest
        .newBuilder()
        .uri(uri)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
{
  "name": "my category",
  "parent": "97458c07-75c0-4f9d-a06e-92d8cdf67c40"
}
        """))
        .build();
var response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Content-Type": []string{"application/json"},
        "Accept": []string{"application/json"},
        "Authorization": []string{"Bearer {access-token}"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "http://localhost:21721/category/add", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

```shell
# You can also use wget
curl -X POST http://localhost:21721/category/add \
  -H 'Content-Type: application/json' \  -H 'Accept: application/json' \  -H 'Authorization: Bearer {access-token}' \
  --data '
{
  "name": "my category",
  "parent": "97458c07-75c0-4f9d-a06e-92d8cdf67c40"
}
'

```

</details>

## Remove connection

<a id="opIdconnectionRemove"></a>

`POST /connection/remove`

Removes a set of connection. This includes any possible children associated with the connection.

Some connections, for example the local machine, can not be removed.

> Body parameter

```json
{
  "connections": [
    "36ad9716-a209-4f7f-9814-078d3349280c"
  ]
}
```

<h3 id="remove-connection-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[ConnectionRemoveRequest](#schemaconnectionremoverequest)|true|none|

> Example responses

> 400 Response

```json
{
  "message": "string"
}
```

<h3 id="remove-connection-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The removal was successful.|None|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|[ClientErrorResponse](#schemaclienterrorresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|[ServerErrorResponse](#schemaservererrorresponse)|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "connections": [
    "36ad9716-a209-4f7f-9814-078d3349280c"
  ]
}';
const headers = {
  'Content-Type':'application/json',
  'Accept':'application/json',
  'Authorization':'Bearer {access-token}'
};

fetch('http://localhost:21721/connection/remove',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```python
import requests
headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json',
  'Authorization': 'Bearer {access-token}'
}

data = """
{
  "connections": [
    "36ad9716-a209-4f7f-9814-078d3349280c"
  ]
}
"""
r = requests.post('http://localhost:21721/connection/remove', headers = headers, data = data)

print(r.json())

```

```java
var uri = URI.create("http://localhost:21721/connection/remove");
var client = HttpClient.newHttpClient();
var request = HttpRequest
        .newBuilder()
        .uri(uri)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
{
  "connections": [
    "36ad9716-a209-4f7f-9814-078d3349280c"
  ]
}
        """))
        .build();
var response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Content-Type": []string{"application/json"},
        "Accept": []string{"application/json"},
        "Authorization": []string{"Bearer {access-token}"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "http://localhost:21721/connection/remove", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

```shell
# You can also use wget
curl -X POST http://localhost:21721/connection/remove \
  -H 'Content-Type: application/json' \  -H 'Accept: application/json' \  -H 'Authorization: Bearer {access-token}' \
  --data '
{
  "connections": [
    "36ad9716-a209-4f7f-9814-078d3349280c"
  ]
}
'

```

</details>

## Open connection in file browser

<a id="opIdconnectionBrowse"></a>

`POST /connection/browse`

Creates a new tab in the file browser and opens the specified connections with an optional starting directory.

> Body parameter

```json
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}
```

<h3 id="open-connection-in-file-browser-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[ConnectionBrowseRequest](#schemaconnectionbrowserequest)|true|none|

> Example responses

> 400 Response

```json
{
  "message": "string"
}
```

<h3 id="open-connection-in-file-browser-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The request was successful. The connection was opened.|None|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|[ClientErrorResponse](#schemaclienterrorresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|[ServerErrorResponse](#schemaservererrorresponse)|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}';
const headers = {
  'Content-Type':'application/json',
  'Accept':'application/json',
  'Authorization':'Bearer {access-token}'
};

fetch('http://localhost:21721/connection/browse',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```python
import requests
headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json',
  'Authorization': 'Bearer {access-token}'
}

data = """
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}
"""
r = requests.post('http://localhost:21721/connection/browse', headers = headers, data = data)

print(r.json())

```

```java
var uri = URI.create("http://localhost:21721/connection/browse");
var client = HttpClient.newHttpClient();
var request = HttpRequest
        .newBuilder()
        .uri(uri)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}
        """))
        .build();
var response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Content-Type": []string{"application/json"},
        "Accept": []string{"application/json"},
        "Authorization": []string{"Bearer {access-token}"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "http://localhost:21721/connection/browse", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

```shell
# You can also use wget
curl -X POST http://localhost:21721/connection/browse \
  -H 'Content-Type: application/json' \  -H 'Accept: application/json' \  -H 'Authorization: Bearer {access-token}' \
  --data '
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}
'

```

</details>

## Open terminal for shell connection

<a id="opIdconnectionTerminal"></a>

`POST /connection/terminal`

Launches a new terminal session for a connection with an optional specified working directory.

> Body parameter

```json
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}
```

<h3 id="open-terminal-for-shell-connection-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[ConnectionTerminalRequest](#schemaconnectionterminalrequest)|true|none|

> Example responses

> 400 Response

```json
{
  "message": "string"
}
```

<h3 id="open-terminal-for-shell-connection-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The request was successful. The connection was opened.|None|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|[ClientErrorResponse](#schemaclienterrorresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|[ServerErrorResponse](#schemaservererrorresponse)|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}';
const headers = {
  'Content-Type':'application/json',
  'Accept':'application/json',
  'Authorization':'Bearer {access-token}'
};

fetch('http://localhost:21721/connection/terminal',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```python
import requests
headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json',
  'Authorization': 'Bearer {access-token}'
}

data = """
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}
"""
r = requests.post('http://localhost:21721/connection/terminal', headers = headers, data = data)

print(r.json())

```

```java
var uri = URI.create("http://localhost:21721/connection/terminal");
var client = HttpClient.newHttpClient();
var request = HttpRequest
        .newBuilder()
        .uri(uri)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}
        """))
        .build();
var response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Content-Type": []string{"application/json"},
        "Accept": []string{"application/json"},
        "Authorization": []string{"Bearer {access-token}"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "http://localhost:21721/connection/terminal", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

```shell
# You can also use wget
curl -X POST http://localhost:21721/connection/terminal \
  -H 'Content-Type: application/json' \  -H 'Accept: application/json' \  -H 'Authorization: Bearer {access-token}' \
  --data '
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}
'

```

</details>

## Toggle state of a connection

<a id="opIdconnectionToggle"></a>

`POST /connection/toggle`

Updates the state of a connection to either start or stop a session.

This can be used for all kinds of services and tunnels.

> Body parameter

```json
{
  "connection": "36ad9716-a209-4f7f-9814-078d3349280c",
  "state": true
}
```

<h3 id="toggle-state-of-a-connection-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[ConnectionToggleRequest](#schemaconnectiontogglerequest)|true|none|

> Example responses

> 400 Response

```json
{
  "message": "string"
}
```

<h3 id="toggle-state-of-a-connection-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The request was successful. The connection state was updated.|None|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|[ClientErrorResponse](#schemaclienterrorresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|[ServerErrorResponse](#schemaservererrorresponse)|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "connection": "36ad9716-a209-4f7f-9814-078d3349280c",
  "state": true
}';
const headers = {
  'Content-Type':'application/json',
  'Accept':'application/json',
  'Authorization':'Bearer {access-token}'
};

fetch('http://localhost:21721/connection/toggle',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```python
import requests
headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json',
  'Authorization': 'Bearer {access-token}'
}

data = """
{
  "connection": "36ad9716-a209-4f7f-9814-078d3349280c",
  "state": true
}
"""
r = requests.post('http://localhost:21721/connection/toggle', headers = headers, data = data)

print(r.json())

```

```java
var uri = URI.create("http://localhost:21721/connection/toggle");
var client = HttpClient.newHttpClient();
var request = HttpRequest
        .newBuilder()
        .uri(uri)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
{
  "connection": "36ad9716-a209-4f7f-9814-078d3349280c",
  "state": true
}
        """))
        .build();
var response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Content-Type": []string{"application/json"},
        "Accept": []string{"application/json"},
        "Authorization": []string{"Bearer {access-token}"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "http://localhost:21721/connection/toggle", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

```shell
# You can also use wget
curl -X POST http://localhost:21721/connection/toggle \
  -H 'Content-Type: application/json' \  -H 'Accept: application/json' \  -H 'Authorization: Bearer {access-token}' \
  --data '
{
  "connection": "36ad9716-a209-4f7f-9814-078d3349280c",
  "state": true
}
'

```

</details>

## Refresh state of a connection

<a id="opIdconnectionRefresh"></a>

`POST /connection/refresh`

Performs a refresh on the specified connection.

This will update the connection state information and also any children if the connection type has any.

> Body parameter

```json
{
  "connection": "36ad9716-a209-4f7f-9814-078d3349280c"
}
```

<h3 id="refresh-state-of-a-connection-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[ConnectionRefreshRequest](#schemaconnectionrefreshrequest)|true|none|

> Example responses

> 400 Response

```json
{
  "message": "string"
}
```

<h3 id="refresh-state-of-a-connection-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The request was successful. The connection state was updated.|None|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|[ClientErrorResponse](#schemaclienterrorresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|[ServerErrorResponse](#schemaservererrorresponse)|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "connection": "36ad9716-a209-4f7f-9814-078d3349280c"
}';
const headers = {
  'Content-Type':'application/json',
  'Accept':'application/json',
  'Authorization':'Bearer {access-token}'
};

fetch('http://localhost:21721/connection/refresh',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```python
import requests
headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json',
  'Authorization': 'Bearer {access-token}'
}

data = """
{
  "connection": "36ad9716-a209-4f7f-9814-078d3349280c"
}
"""
r = requests.post('http://localhost:21721/connection/refresh', headers = headers, data = data)

print(r.json())

```

```java
var uri = URI.create("http://localhost:21721/connection/refresh");
var client = HttpClient.newHttpClient();
var request = HttpRequest
        .newBuilder()
        .uri(uri)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
{
  "connection": "36ad9716-a209-4f7f-9814-078d3349280c"
}
        """))
        .build();
var response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Content-Type": []string{"application/json"},
        "Accept": []string{"application/json"},
        "Authorization": []string{"Bearer {access-token}"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "http://localhost:21721/connection/refresh", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

```shell
# You can also use wget
curl -X POST http://localhost:21721/connection/refresh \
  -H 'Content-Type: application/json' \  -H 'Accept: application/json' \  -H 'Authorization: Bearer {access-token}' \
  --data '
{
  "connection": "36ad9716-a209-4f7f-9814-078d3349280c"
}
'

```

</details>

## Start shell connection

<a id="opIdshellStart"></a>

`POST /shell/start`

Starts a new shell session for a connection. If an existing shell session is already running for that connection, this operation will do nothing.

Note that there are a variety of possible errors that can occur here when establishing the shell connection.
These errors will be returned with the HTTP return code 500.

> Body parameter

```json
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}
```

<h3 id="start-shell-connection-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[ShellStartRequest](#schemashellstartrequest)|true|none|

> Example responses

> 200 Response

```json
{
  "shellDialect": 0,
  "osType": "string",
  "osName": "string",
  "ttyState": "string",
  "temp": "string"
}
```

<h3 id="start-shell-connection-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The operation was successful. The shell session was started.|[ShellStartResponse](#schemashellstartresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|[ClientErrorResponse](#schemaclienterrorresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|[ServerErrorResponse](#schemaservererrorresponse)|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}';
const headers = {
  'Content-Type':'application/json',
  'Accept':'application/json',
  'Authorization':'Bearer {access-token}'
};

fetch('http://localhost:21721/shell/start',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```python
import requests
headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json',
  'Authorization': 'Bearer {access-token}'
}

data = """
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}
"""
r = requests.post('http://localhost:21721/shell/start', headers = headers, data = data)

print(r.json())

```

```java
var uri = URI.create("http://localhost:21721/shell/start");
var client = HttpClient.newHttpClient();
var request = HttpRequest
        .newBuilder()
        .uri(uri)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}
        """))
        .build();
var response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Content-Type": []string{"application/json"},
        "Accept": []string{"application/json"},
        "Authorization": []string{"Bearer {access-token}"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "http://localhost:21721/shell/start", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

```shell
# You can also use wget
curl -X POST http://localhost:21721/shell/start \
  -H 'Content-Type: application/json' \  -H 'Accept: application/json' \  -H 'Authorization: Bearer {access-token}' \
  --data '
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}
'

```

</details>

## Stop shell connection

<a id="opIdshellStop"></a>

`POST /shell/stop`

Stops an existing shell session for a connection.

This operation will return once the shell has exited.
If the shell is busy or stuck, you might have to work with timeouts to account for these cases.

> Body parameter

```json
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}
```

<h3 id="stop-shell-connection-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[ShellStopRequest](#schemashellstoprequest)|true|none|

> Example responses

> 400 Response

```json
{
  "message": "string"
}
```

<h3 id="stop-shell-connection-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The operation was successful. The shell session was stopped.|None|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|[ClientErrorResponse](#schemaclienterrorresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|[ServerErrorResponse](#schemaservererrorresponse)|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}';
const headers = {
  'Content-Type':'application/json',
  'Accept':'application/json',
  'Authorization':'Bearer {access-token}'
};

fetch('http://localhost:21721/shell/stop',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```python
import requests
headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json',
  'Authorization': 'Bearer {access-token}'
}

data = """
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}
"""
r = requests.post('http://localhost:21721/shell/stop', headers = headers, data = data)

print(r.json())

```

```java
var uri = URI.create("http://localhost:21721/shell/stop");
var client = HttpClient.newHttpClient();
var request = HttpRequest
        .newBuilder()
        .uri(uri)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}
        """))
        .build();
var response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Content-Type": []string{"application/json"},
        "Accept": []string{"application/json"},
        "Authorization": []string{"Bearer {access-token}"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "http://localhost:21721/shell/stop", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

```shell
# You can also use wget
curl -X POST http://localhost:21721/shell/stop \
  -H 'Content-Type: application/json' \  -H 'Accept: application/json' \  -H 'Authorization: Bearer {access-token}' \
  --data '
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}
'

```

</details>

## Execute command in a shell session

<a id="opIdshellExec"></a>

`POST /shell/exec`

Runs a command in an active shell session and waits for it to finish. The exit code and output will be returned in the response.

Note that a variety of different errors can occur when executing the command.
If the command finishes, even with an error code, a normal HTTP 200 response will be returned.
However, if any other error occurs like the shell not responding or exiting unexpectedly, an HTTP 500 response will be returned.

> Body parameter

```json
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "command": "echo $USER"
}
```

<h3 id="execute-command-in-a-shell-session-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[ShellExecRequest](#schemashellexecrequest)|true|none|

> Example responses

> The operation was successful. The shell command finished.

```json
{
  "exitCode": 0,
  "stdout": "root",
  "stderr": ""
}
```

```json
{
  "exitCode": 127,
  "stdout": "",
  "stderr": "invalid: command not found"
}
```

> 400 Response

```json
{
  "message": "string"
}
```

<h3 id="execute-command-in-a-shell-session-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The operation was successful. The shell command finished.|[ShellExecResponse](#schemashellexecresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|[ClientErrorResponse](#schemaclienterrorresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|[ServerErrorResponse](#schemaservererrorresponse)|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "command": "echo $USER"
}';
const headers = {
  'Content-Type':'application/json',
  'Accept':'application/json',
  'Authorization':'Bearer {access-token}'
};

fetch('http://localhost:21721/shell/exec',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```python
import requests
headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json',
  'Authorization': 'Bearer {access-token}'
}

data = """
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "command": "echo $USER"
}
"""
r = requests.post('http://localhost:21721/shell/exec', headers = headers, data = data)

print(r.json())

```

```java
var uri = URI.create("http://localhost:21721/shell/exec");
var client = HttpClient.newHttpClient();
var request = HttpRequest
        .newBuilder()
        .uri(uri)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "command": "echo $USER"
}
        """))
        .build();
var response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Content-Type": []string{"application/json"},
        "Accept": []string{"application/json"},
        "Authorization": []string{"Bearer {access-token}"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "http://localhost:21721/shell/exec", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

```shell
# You can also use wget
curl -X POST http://localhost:21721/shell/exec \
  -H 'Content-Type: application/json' \  -H 'Accept: application/json' \  -H 'Authorization: Bearer {access-token}' \
  --data '
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "command": "echo $USER"
}
'

```

</details>

## Store a raw blob to be used later

<a id="opIdfsData"></a>

`POST /fs/blob`

Stores arbitrary binary data in a blob such that it can be used later on to for example write to a remote file.

This will return a uuid which can be used as a reference to the blob.
You can also store normal text data in blobs if you intend to create text or shell script files with it.

> Body parameter

```yaml
string

```

<h3 id="store-a-raw-blob-to-be-used-later-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|string(binary)|true|none|

> Example responses

> The operation was successful. The data was stored.

```json
{
  "blob": "854afc45-eadc-49a0-a45d-9fb76a484304"
}
```

> 400 Response

```json
{
  "message": "string"
}
```

<h3 id="store-a-raw-blob-to-be-used-later-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The operation was successful. The data was stored.|[FsBlobResponse](#schemafsblobresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|[ClientErrorResponse](#schemaclienterrorresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|[ServerErrorResponse](#schemaservererrorresponse)|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = 'string';
const headers = {
  'Content-Type':'application/octet-stream',
  'Accept':'application/json',
  'Authorization':'Bearer {access-token}'
};

fetch('http://localhost:21721/fs/blob',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```python
import requests
headers = {
  'Content-Type': 'application/octet-stream',
  'Accept': 'application/json',
  'Authorization': 'Bearer {access-token}'
}

data = """
string
"""
r = requests.post('http://localhost:21721/fs/blob', headers = headers, data = data)

print(r.json())

```

```java
var uri = URI.create("http://localhost:21721/fs/blob");
var client = HttpClient.newHttpClient();
var request = HttpRequest
        .newBuilder()
        .uri(uri)
        .header("Content-Type", "application/octet-stream")
        .header("Accept", "application/json")
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
string
        """))
        .build();
var response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Content-Type": []string{"application/octet-stream"},
        "Accept": []string{"application/json"},
        "Authorization": []string{"Bearer {access-token}"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "http://localhost:21721/fs/blob", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

```shell
# You can also use wget
curl -X POST http://localhost:21721/fs/blob \
  -H 'Content-Type: application/octet-stream' \  -H 'Accept: application/json' \  -H 'Authorization: Bearer {access-token}' \
  --data '
string
'

```

</details>

## Read the content of a remote file

<a id="opIdfsRead"></a>

`POST /fs/read`

Reads the entire content of a remote file through an active shell session.

> Body parameter

```json
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "path": "/home/user/myfile.txt"
}
```

<h3 id="read-the-content-of-a-remote-file-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[FsReadRequest](#schemafsreadrequest)|true|none|

> Example responses

> 200 Response

> 400 Response

```json
{
  "message": "string"
}
```

<h3 id="read-the-content-of-a-remote-file-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The operation was successful. The file was read.|string|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|[ClientErrorResponse](#schemaclienterrorresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|[ServerErrorResponse](#schemaservererrorresponse)|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "path": "/home/user/myfile.txt"
}';
const headers = {
  'Content-Type':'application/json',
  'Accept':'application/octet-stream',
  'Authorization':'Bearer {access-token}'
};

fetch('http://localhost:21721/fs/read',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```python
import requests
headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/octet-stream',
  'Authorization': 'Bearer {access-token}'
}

data = """
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "path": "/home/user/myfile.txt"
}
"""
r = requests.post('http://localhost:21721/fs/read', headers = headers, data = data)

print(r.json())

```

```java
var uri = URI.create("http://localhost:21721/fs/read");
var client = HttpClient.newHttpClient();
var request = HttpRequest
        .newBuilder()
        .uri(uri)
        .header("Content-Type", "application/json")
        .header("Accept", "application/octet-stream")
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "path": "/home/user/myfile.txt"
}
        """))
        .build();
var response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Content-Type": []string{"application/json"},
        "Accept": []string{"application/octet-stream"},
        "Authorization": []string{"Bearer {access-token}"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "http://localhost:21721/fs/read", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

```shell
# You can also use wget
curl -X POST http://localhost:21721/fs/read \
  -H 'Content-Type: application/json' \  -H 'Accept: application/octet-stream' \  -H 'Authorization: Bearer {access-token}' \
  --data '
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "path": "/home/user/myfile.txt"
}
'

```

</details>

## Write a blob to a remote file

<a id="opIdfsWrite"></a>

`POST /fs/write`

Writes blob data to a file through an active shell session.

> Body parameter

```json
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "blob": "854afc45-eadc-49a0-a45d-9fb76a484304",
  "path": "/home/user/myfile.txt"
}
```

<h3 id="write-a-blob-to-a-remote-file-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[FsWriteRequest](#schemafswriterequest)|true|none|

> Example responses

> 400 Response

```json
{
  "message": "string"
}
```

<h3 id="write-a-blob-to-a-remote-file-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The operation was successful. The file was written.|None|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|[ClientErrorResponse](#schemaclienterrorresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|[ServerErrorResponse](#schemaservererrorresponse)|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "blob": "854afc45-eadc-49a0-a45d-9fb76a484304",
  "path": "/home/user/myfile.txt"
}';
const headers = {
  'Content-Type':'application/json',
  'Accept':'application/json',
  'Authorization':'Bearer {access-token}'
};

fetch('http://localhost:21721/fs/write',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```python
import requests
headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json',
  'Authorization': 'Bearer {access-token}'
}

data = """
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "blob": "854afc45-eadc-49a0-a45d-9fb76a484304",
  "path": "/home/user/myfile.txt"
}
"""
r = requests.post('http://localhost:21721/fs/write', headers = headers, data = data)

print(r.json())

```

```java
var uri = URI.create("http://localhost:21721/fs/write");
var client = HttpClient.newHttpClient();
var request = HttpRequest
        .newBuilder()
        .uri(uri)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "blob": "854afc45-eadc-49a0-a45d-9fb76a484304",
  "path": "/home/user/myfile.txt"
}
        """))
        .build();
var response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Content-Type": []string{"application/json"},
        "Accept": []string{"application/json"},
        "Authorization": []string{"Bearer {access-token}"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "http://localhost:21721/fs/write", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

```shell
# You can also use wget
curl -X POST http://localhost:21721/fs/write \
  -H 'Content-Type: application/json' \  -H 'Accept: application/json' \  -H 'Authorization: Bearer {access-token}' \
  --data '
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "blob": "854afc45-eadc-49a0-a45d-9fb76a484304",
  "path": "/home/user/myfile.txt"
}
'

```

</details>

## Create a shell script file from a blob

<a id="opIdfsScript"></a>

`POST /fs/script`

Creates a shell script in the temporary directory of the file system that is access through the shell connection.

This can be used to run more complex commands on remote systems.

> Body parameter

```json
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "blob": "854afc45-eadc-49a0-a45d-9fb76a484304"
}
```

<h3 id="create-a-shell-script-file-from-a-blob-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[FsScriptRequest](#schemafsscriptrequest)|true|none|

> Example responses

> The operation was successful. The script file was created.

```json
{
  "path": "/tmp/xpipe-123.sh"
}
```

> 400 Response

```json
{
  "message": "string"
}
```

<h3 id="create-a-shell-script-file-from-a-blob-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The operation was successful. The script file was created.|[FsScriptResponse](#schemafsscriptresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|[ClientErrorResponse](#schemaclienterrorresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|[ServerErrorResponse](#schemaservererrorresponse)|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "blob": "854afc45-eadc-49a0-a45d-9fb76a484304"
}';
const headers = {
  'Content-Type':'application/json',
  'Accept':'application/json',
  'Authorization':'Bearer {access-token}'
};

fetch('http://localhost:21721/fs/script',
{
  method: 'POST',
  body: inputBody,
  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```python
import requests
headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json',
  'Authorization': 'Bearer {access-token}'
}

data = """
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "blob": "854afc45-eadc-49a0-a45d-9fb76a484304"
}
"""
r = requests.post('http://localhost:21721/fs/script', headers = headers, data = data)

print(r.json())

```

```java
var uri = URI.create("http://localhost:21721/fs/script");
var client = HttpClient.newHttpClient();
var request = HttpRequest
        .newBuilder()
        .uri(uri)
        .header("Content-Type", "application/json")
        .header("Accept", "application/json")
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "blob": "854afc45-eadc-49a0-a45d-9fb76a484304"
}
        """))
        .build();
var response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Content-Type": []string{"application/json"},
        "Accept": []string{"application/json"},
        "Authorization": []string{"Bearer {access-token}"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "http://localhost:21721/fs/script", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

```shell
# You can also use wget
curl -X POST http://localhost:21721/fs/script \
  -H 'Content-Type: application/json' \  -H 'Accept: application/json' \  -H 'Authorization: Bearer {access-token}' \
  --data '
{
  "connection": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "blob": "854afc45-eadc-49a0-a45d-9fb76a484304"
}
'

```

</details>

## Query daemon version

<a id="opIddaemonVersion"></a>

`POST /daemon/version`

Retrieves version information from the daemon

> Example responses

> 200 Response

```json
{
  "version": "string",
  "canonicalVersion": "string",
  "buildVersion": "string",
  "jvmVersion": "string",
  "pro": true
}
```

<h3 id="query-daemon-version-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The operation was successful|[DaemonVersionResponse](#schemadaemonversionresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|[ClientErrorResponse](#schemaclienterrorresponse)|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|[ServerErrorResponse](#schemaservererrorresponse)|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript

const headers = {
  'Accept':'application/json',
  'Authorization':'Bearer {access-token}'
};

fetch('http://localhost:21721/daemon/version',
{
  method: 'POST',

  headers: headers
})
.then(function(res) {
    return res.json();
}).then(function(body) {
    console.log(body);
});

```

```python
import requests
headers = {
  'Accept': 'application/json',
  'Authorization': 'Bearer {access-token}'
}

data = """
undefined
"""
r = requests.post('http://localhost:21721/daemon/version', headers = headers, data = data)

print(r.json())

```

```java
var uri = URI.create("http://localhost:21721/daemon/version");
var client = HttpClient.newHttpClient();
var request = HttpRequest
        .newBuilder()
        .uri(uri)
        .header("Accept", "application/json")
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
undefined
        """))
        .build();
var response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.statusCode());
System.out.println(response.body());

```

```go
package main

import (
       "bytes"
       "net/http"
)

func main() {

    headers := map[string][]string{
        "Accept": []string{"application/json"},
        "Authorization": []string{"Bearer {access-token}"},
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "http://localhost:21721/daemon/version", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

```shell
# You can also use wget
curl -X POST http://localhost:21721/daemon/version \
  -H 'Accept: application/json' \  -H 'Authorization: Bearer {access-token}' \
  --data '
undefined
'

```

</details>

# Schemas

<h2 id="tocS_ShellStartRequest">ShellStartRequest</h2>

<a id="schemashellstartrequest"></a>
<a id="schema_ShellStartRequest"></a>
<a id="tocSshellstartrequest"></a>
<a id="tocsshellstartrequest"></a>

```json
{
  "connection": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|connection|string|true|none|The connection uuid|

<h2 id="tocS_ShellStartResponse">ShellStartResponse</h2>

<a id="schemashellstartresponse"></a>
<a id="schema_ShellStartResponse"></a>
<a id="tocSshellstartresponse"></a>
<a id="tocsshellstartresponse"></a>

```json
{
  "shellDialect": 0,
  "osType": "string",
  "osName": "string",
  "ttyState": "string",
  "temp": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|shellDialect|integer|true|none|The shell dialect|
|osType|string|true|none|The general type of operating system|
|osName|string|true|none|The display name of the operating system|
|ttyState|string|false|none|Whether a tty/pty has been allocated for the connection. If allocated, input and output will be unreliable. It is not recommended to use a shell connection then.|
|temp|string|true|none|The location of the temporary directory|

<h2 id="tocS_ShellStopRequest">ShellStopRequest</h2>

<a id="schemashellstoprequest"></a>
<a id="schema_ShellStopRequest"></a>
<a id="tocSshellstoprequest"></a>
<a id="tocsshellstoprequest"></a>

```json
{
  "connection": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|connection|string|true|none|The connection uuid|

<h2 id="tocS_ShellExecRequest">ShellExecRequest</h2>

<a id="schemashellexecrequest"></a>
<a id="schema_ShellExecRequest"></a>
<a id="tocSshellexecrequest"></a>
<a id="tocsshellexecrequest"></a>

```json
{
  "connection": "string",
  "command": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|connection|string|true|none|The connection uuid|
|command|string|true|none|The command to execute|

<h2 id="tocS_ShellExecResponse">ShellExecResponse</h2>

<a id="schemashellexecresponse"></a>
<a id="schema_ShellExecResponse"></a>
<a id="tocSshellexecresponse"></a>
<a id="tocsshellexecresponse"></a>

```json
{
  "exitCode": 0,
  "stdout": "string",
  "stderr": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|exitCode|integer|true|none|The exit code of the command|
|stdout|string|true|none|The stdout output of the command|
|stderr|string|true|none|The stderr output of the command|

<h2 id="tocS_FsBlobResponse">FsBlobResponse</h2>

<a id="schemafsblobresponse"></a>
<a id="schema_FsBlobResponse"></a>
<a id="tocSfsblobresponse"></a>
<a id="tocsfsblobresponse"></a>

```json
{
  "blob": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|blob|string|true|none|The data uuid|

<h2 id="tocS_FsWriteRequest">FsWriteRequest</h2>

<a id="schemafswriterequest"></a>
<a id="schema_FsWriteRequest"></a>
<a id="tocSfswriterequest"></a>
<a id="tocsfswriterequest"></a>

```json
{
  "connection": "string",
  "blob": "string",
  "path": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|connection|string|true|none|The connection uuid|
|blob|string|true|none|The blob uuid|
|path|string|true|none|The target filepath|

<h2 id="tocS_FsReadRequest">FsReadRequest</h2>

<a id="schemafsreadrequest"></a>
<a id="schema_FsReadRequest"></a>
<a id="tocSfsreadrequest"></a>
<a id="tocsfsreadrequest"></a>

```json
{
  "connection": "string",
  "path": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|connection|string|true|none|The connection uuid|
|path|string|true|none|The target file path|

<h2 id="tocS_FsScriptRequest">FsScriptRequest</h2>

<a id="schemafsscriptrequest"></a>
<a id="schema_FsScriptRequest"></a>
<a id="tocSfsscriptrequest"></a>
<a id="tocsfsscriptrequest"></a>

```json
{
  "connection": "string",
  "blob": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|connection|string|true|none|The connection uuid|
|blob|string|true|none|The blob uuid|

<h2 id="tocS_FsScriptResponse">FsScriptResponse</h2>

<a id="schemafsscriptresponse"></a>
<a id="schema_FsScriptResponse"></a>
<a id="tocSfsscriptresponse"></a>
<a id="tocsfsscriptresponse"></a>

```json
{
  "path": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|path|string|true|none|The generated script file path|

<h2 id="tocS_ConnectionQueryRequest">ConnectionQueryRequest</h2>

<a id="schemaconnectionqueryrequest"></a>
<a id="schema_ConnectionQueryRequest"></a>
<a id="tocSconnectionqueryrequest"></a>
<a id="tocsconnectionqueryrequest"></a>

```json
{
  "categoryFilter": "string",
  "connectionFilter": "string",
  "typeFilter": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|categoryFilter|string|true|none|The filter string to match categories. Categories are delimited by / if they are hierarchical. The filter supports globs.|
|connectionFilter|string|true|none|The filter string to match connection names. Connection names are delimited by / if they are hierarchical. The filter supports globs.|
|typeFilter|string|true|none|The filter string to connection types. Every unique type of connection like SSH or docker has its own type identifier that you can match. The filter supports globs.|

<h2 id="tocS_ConnectionQueryResponse">ConnectionQueryResponse</h2>

<a id="schemaconnectionqueryresponse"></a>
<a id="schema_ConnectionQueryResponse"></a>
<a id="tocSconnectionqueryresponse"></a>
<a id="tocsconnectionqueryresponse"></a>

```json
{
  "found": [
    "string"
  ]
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|found|[string]|true|none|The found connections|

<h2 id="tocS_ConnectionInfoRequest">ConnectionInfoRequest</h2>

<a id="schemaconnectioninforequest"></a>
<a id="schema_ConnectionInfoRequest"></a>
<a id="tocSconnectioninforequest"></a>
<a id="tocsconnectioninforequest"></a>

```json
{
  "connections": [
    "string"
  ]
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|connections|[string]|true|none|The connections|

<h2 id="tocS_ConnectionInfoResponse">ConnectionInfoResponse</h2>

<a id="schemaconnectioninforesponse"></a>
<a id="schema_ConnectionInfoResponse"></a>
<a id="tocSconnectioninforesponse"></a>
<a id="tocsconnectioninforesponse"></a>

```json
[
  {
    "connection": "string",
    "category": [
      "string"
    ],
    "name": [
      "string"
    ],
    "type": "string",
    "rawData": {},
    "usageCategory": "shell",
    "lastModified": "string",
    "lastUsed": "string",
    "state": {},
    "cache": {}
  }
]

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|connection|string|true|none|The unique id of the connection|
|category|[string]|true|none|The full category path as an array|
|name|[string]|true|none|The full connection name path as an array|
|type|string|true|none|The type identifier of the connection|
|rawData|object|true|none|The raw internal configuration data for the connection. The schema for these is internal and should not be relied upon.|
|usageCategory|string|true|none|The category of how this connection can be used.|
|lastModified|string|true|none|The timestamp of when the connection configuration was last modified in ISO 8601|
|lastUsed|string|true|none|The timestamp of when the connection was last launched in ISO 8601|
|state|object|true|none|The internal persistent state information about the connection|
|cache|object|true|none|The temporary cache data for the connection|

#### Enumerated Values

|Property|Value|
|---|---|
|usageCategory|shell|
|usageCategory|tunnel|
|usageCategory|script|
|usageCategory|database|
|usageCategory|command|
|usageCategory|desktop|
|usageCategory|group|

<h2 id="tocS_ConnectionRefreshRequest">ConnectionRefreshRequest</h2>

<a id="schemaconnectionrefreshrequest"></a>
<a id="schema_ConnectionRefreshRequest"></a>
<a id="tocSconnectionrefreshrequest"></a>
<a id="tocsconnectionrefreshrequest"></a>

```json
{
  "connection": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|connection|string|true|none|The connection uuid|

<h2 id="tocS_ConnectionAddRequest">ConnectionAddRequest</h2>

<a id="schemaconnectionaddrequest"></a>
<a id="schema_ConnectionAddRequest"></a>
<a id="tocSconnectionaddrequest"></a>
<a id="tocsconnectionaddrequest"></a>

```json
{
  "name": "string",
  "data": {},
  "validate": true,
  "category": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|name|string|true|none|The connection name|
|data|object|true|none|The raw connection store data. Schemas for connection types are not documented, but you can find the connection data of your existing connections in the xpipe vault.|
|validate|boolean|true|none|Whether to perform a connection validation before adding it, i.e., probe the connection first. If validation is enabled and fails, the connection will not be added|
|category|string|false|none|The category uuid to put the connection in. If not specified, the default category will be used|

<h2 id="tocS_ConnectionAddResponse">ConnectionAddResponse</h2>

<a id="schemaconnectionaddresponse"></a>
<a id="schema_ConnectionAddResponse"></a>
<a id="tocSconnectionaddresponse"></a>
<a id="tocsconnectionaddresponse"></a>

```json
{
  "connection": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|connection|string|true|none|The connection uuid|

<h2 id="tocS_CategoryAddRequest">CategoryAddRequest</h2>

<a id="schemacategoryaddrequest"></a>
<a id="schema_CategoryAddRequest"></a>
<a id="tocScategoryaddrequest"></a>
<a id="tocscategoryaddrequest"></a>

```json
{
  "name": "string",
  "parent": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|name|string|true|none|The category name|
|parent|string|true|none|The parent category uuid to put the new category in|

<h2 id="tocS_CategoryAddResponse">CategoryAddResponse</h2>

<a id="schemacategoryaddresponse"></a>
<a id="schema_CategoryAddResponse"></a>
<a id="tocScategoryaddresponse"></a>
<a id="tocscategoryaddresponse"></a>

```json
{
  "category": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|category|string|true|none|The category uuid|

<h2 id="tocS_ConnectionRemoveRequest">ConnectionRemoveRequest</h2>

<a id="schemaconnectionremoverequest"></a>
<a id="schema_ConnectionRemoveRequest"></a>
<a id="tocSconnectionremoverequest"></a>
<a id="tocsconnectionremoverequest"></a>

```json
{
  "connections": [
    "string"
  ]
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|connections|[string]|true|none|The connections to remove|

<h2 id="tocS_ConnectionBrowseRequest">ConnectionBrowseRequest</h2>

<a id="schemaconnectionbrowserequest"></a>
<a id="schema_ConnectionBrowseRequest"></a>
<a id="tocSconnectionbrowserequest"></a>
<a id="tocsconnectionbrowserequest"></a>

```json
{
  "directory": "string",
  "connection": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|directory|string|true|none|The optional directory to browse to|
|connection|string|true|none|The connection uuid|

<h2 id="tocS_ConnectionToggleRequest">ConnectionToggleRequest</h2>

<a id="schemaconnectiontogglerequest"></a>
<a id="schema_ConnectionToggleRequest"></a>
<a id="tocSconnectiontogglerequest"></a>
<a id="tocsconnectiontogglerequest"></a>

```json
{
  "state": true,
  "connection": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|state|boolean|true|none|The state to switch to|
|connection|string|true|none|The connection uuid|

<h2 id="tocS_ConnectionTerminalRequest">ConnectionTerminalRequest</h2>

<a id="schemaconnectionterminalrequest"></a>
<a id="schema_ConnectionTerminalRequest"></a>
<a id="tocSconnectionterminalrequest"></a>
<a id="tocsconnectionterminalrequest"></a>

```json
{
  "directory": "string",
  "connection": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|directory|string|true|none|The optional directory to use as the working directory|
|connection|string|true|none|The connection uuid|

<h2 id="tocS_HandshakeRequest">HandshakeRequest</h2>

<a id="schemahandshakerequest"></a>
<a id="schema_HandshakeRequest"></a>
<a id="tocShandshakerequest"></a>
<a id="tocshandshakerequest"></a>

```json
{
  "auth": {
    "type": "string",
    "key": "string"
  },
  "client": {
    "type": "string"
  }
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|auth|[AuthMethod](#schemaauthmethod)|true|none|none|
|client|[ClientInformation](#schemaclientinformation)|true|none|none|

<h2 id="tocS_HandshakeResponse">HandshakeResponse</h2>

<a id="schemahandshakeresponse"></a>
<a id="schema_HandshakeResponse"></a>
<a id="tocShandshakeresponse"></a>
<a id="tocshandshakeresponse"></a>

```json
{
  "sessionToken": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|sessionToken|string|true|none|The generated bearer token that can be used for authentication in this session|

<h2 id="tocS_DaemonVersionResponse">DaemonVersionResponse</h2>

<a id="schemadaemonversionresponse"></a>
<a id="schema_DaemonVersionResponse"></a>
<a id="tocSdaemonversionresponse"></a>
<a id="tocsdaemonversionresponse"></a>

```json
{
  "version": "string",
  "canonicalVersion": "string",
  "buildVersion": "string",
  "jvmVersion": "string",
  "pro": true
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|version|string|true|none|The version of the running daemon|
|canonicalVersion|string|true|none|The canonical version of the running daemon|
|buildVersion|string|true|none|The build timestamp|
|jvmVersion|string|true|none|The version of the Java Virtual Machine in which the daemon is running|
|pro|boolean|true|none|Whether the daemon supports professional edition features|

<h2 id="tocS_AuthMethod">AuthMethod</h2>

<a id="schemaauthmethod"></a>
<a id="schema_AuthMethod"></a>
<a id="tocSauthmethod"></a>
<a id="tocsauthmethod"></a>

```json
{
  "type": "string",
  "key": "string"
}

```

<h3>Properties</h3>

oneOf

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|*anonymous*|[ApiKey](#schemaapikey)|false|none|API key authentication|

xor

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|*anonymous*|[Local](#schemalocal)|false|none|Authentication method for local applications. Uses file system access as proof of authentication.<br><br>You can find the authentication file at:<br>- %TEMP%\xpipe_auth on Windows<br>- $TMP/xpipe_auth on Linux<br>- $TMPDIR/xpipe_auth on macOS<br><br>For the PTB releases the file name is changed to xpipe_ptb_auth to prevent collisions.<br><br>As the temporary directory on Linux is global, the daemon might run as another user and your current user might not have permissions to access the auth file.|

<h2 id="tocS_ApiKey">ApiKey</h2>

<a id="schemaapikey"></a>
<a id="schema_ApiKey"></a>
<a id="tocSapikey"></a>
<a id="tocsapikey"></a>

```json
{
  "type": "string",
  "key": "string"
}

```

API key authentication

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|type|string|true|none|none|
|key|string|true|none|The API key|

<h2 id="tocS_Local">Local</h2>

<a id="schemalocal"></a>
<a id="schema_Local"></a>
<a id="tocSlocal"></a>
<a id="tocslocal"></a>

```json
{
  "type": "string",
  "authFileContent": "string"
}

```

Authentication method for local applications. Uses file system access as proof of authentication.

You can find the authentication file at:
- %TEMP%\xpipe_auth on Windows
- $TMP/xpipe_auth on Linux
- $TMPDIR/xpipe_auth on macOS

For the PTB releases the file name is changed to xpipe_ptb_auth to prevent collisions.

As the temporary directory on Linux is global, the daemon might run as another user and your current user might not have permissions to access the auth file.

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|type|string|true|none|none|
|authFileContent|string|true|none|The contents of the local file <temp dir>/xpipe_auth. This file is automatically generated when XPipe starts.|

<h2 id="tocS_ClientInformation">ClientInformation</h2>

<a id="schemaclientinformation"></a>
<a id="schema_ClientInformation"></a>
<a id="tocSclientinformation"></a>
<a id="tocsclientinformation"></a>

```json
{
  "type": "string"
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|type|string|true|none|none|

<h2 id="tocS_ApiClientInformation">ApiClientInformation</h2>

<a id="schemaapiclientinformation"></a>
<a id="schema_ApiClientInformation"></a>
<a id="tocSapiclientinformation"></a>
<a id="tocsapiclientinformation"></a>

```json
{
  "type": "string",
  "name": "string"
}

```

Provides information about the client that connected to the XPipe API.

<h3>Properties</h3>

allOf - discriminator: ClientInformation.type

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|*anonymous*|[ClientInformation](#schemaclientinformation)|false|none|none|

and

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|*anonymous*|object|false|none|none|
|» name|string|true|none|The name of the client.|

<h2 id="tocS_ClientErrorResponse">ClientErrorResponse</h2>

<a id="schemaclienterrorresponse"></a>
<a id="schema_ClientErrorResponse"></a>
<a id="tocSclienterrorresponse"></a>
<a id="tocsclienterrorresponse"></a>

```json
{
  "message": "string"
}

```

Error returned in case of a client exception

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|message|string|true|none|The error message|

<h2 id="tocS_ServerErrorResponse">ServerErrorResponse</h2>

<a id="schemaservererrorresponse"></a>
<a id="schema_ServerErrorResponse"></a>
<a id="tocSservererrorresponse"></a>
<a id="tocsservererrorresponse"></a>

```json
{
  "error": {
    "cause": {},
    "stackTrace": [],
    "suppressed": [],
    "localizedMessage": "string",
    "message": "string"
  }
}

```

Error returned in case of a server exception with HTTP code 500

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|error|object|true|none|The exception information|
|» cause|object|false|none|The exception cause|
|» stackTrace|array|false|none|The java stack trace information|
|» suppressed|array|false|none|Any suppressed exceptions|
|» localizedMessage|string|false|none|Not used|
|» message|string|true|none|The error message|

