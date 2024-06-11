---
title: XPipe API Documentation v10.0
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

<h1 id="xpipe-api-documentation">XPipe API Documentation v10.0</h1>

The XPipe API provides programmatic access to XPipe’s features.

The XPipe application will start up an HTTP server that can be used to send requests.
You can change the port of it in the settings menu.
Note that this server is HTTP-only for now as it runs only on localhost. HTTPS requests are not accepted.

This allows you to programmatically manage remote systems.
To start off, you can query connections based on various filters.
With the matched connections, you can start remote shell sessions for each one and run arbitrary commands in them.
You get the command exit code and output as a response, allowing you to adapt your control flow based on command outputs.
Any kind of passwords and other secrets are automatically provided by XPipe when establishing a shell connection.
If a required password is not stored and is set to be dynamically prompted, the running XPipe application will ask you to enter any required passwords.

You can quickly get started by either using this page as an API reference or alternatively import the [OpenAPI definition file](/openapi.yaml) into your API client of choice.
See the authentication handshake below on how to authenticate prior to sending requests.

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
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|None|

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
    {
      "uuid": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
      "category": [
        "default"
      ],
      "connection": [
        "local machine"
      ],
      "type": "local"
    },
    {
      "uuid": "e1462ddc-9beb-484c-bd91-bb666027e300",
      "category": [
        "default",
        "category 1"
      ],
      "connection": [
        "ssh system",
        "shell environments",
        "bash"
      ],
      "type": "shellEnvironment"
    }
  ]
}
```

<h3 id="query-connections-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The query was successful. The body contains all matched connections.|[ConnectionQueryResponse](#schemaconnectionqueryresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|The requested resource could not be found.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|None|

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

## Start shell connection

<a id="opIdshellStart"></a>

`POST /shell/start`

Starts a new shell session for a connection. If an existing shell session is already running for that connection, this operation will do nothing.

Note that there are a variety of possible errors that can occur here when establishing the shell connection.
These errors will be returned with the HTTP return code 500.

> Body parameter

```json
{
  "uuid": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}
```

<h3 id="start-shell-connection-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[ShellStartRequest](#schemashellstartrequest)|true|none|

<h3 id="start-shell-connection-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The operation was successful. The shell session was started.|None|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|The requested resource could not be found.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|None|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "uuid": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}';
const headers = {
  'Content-Type':'application/json',
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
  'Authorization': 'Bearer {access-token}'
}

data = """
{
  "uuid": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
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
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
{
  "uuid": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
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
  -H 'Content-Type: application/json' \  -H 'Authorization: Bearer {access-token}' \
  --data '
{
  "uuid": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
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
  "uuid": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}
```

<h3 id="stop-shell-connection-parameters">Parameters</h3>

|Name|In|Type|Required|Description|
|---|---|---|---|---|
|body|body|[ShellStopRequest](#schemashellstoprequest)|true|none|

<h3 id="stop-shell-connection-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The operation was successful. The shell session was stopped.|None|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|The requested resource could not be found.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|None|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "uuid": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
}';
const headers = {
  'Content-Type':'application/json',
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
  'Authorization': 'Bearer {access-token}'
}

data = """
{
  "uuid": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
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
        .header("Authorization", "Bearer {access-token}")
        .POST(HttpRequest.BodyPublishers.ofString("""
{
  "uuid": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
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
  -H 'Content-Type: application/json' \  -H 'Authorization: Bearer {access-token}' \
  --data '
{
  "uuid": "f0ec68aa-63f5-405c-b178-9a4454556d6b"
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
  "uuid": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
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

<h3 id="execute-command-in-a-shell-session-responses">Responses</h3>

|Status|Meaning|Description|Schema|
|---|---|---|---|
|200|[OK](https://tools.ietf.org/html/rfc7231#section-6.3.1)|The operation was successful. The shell command finished.|[ShellExecResponse](#schemashellexecresponse)|
|400|[Bad Request](https://tools.ietf.org/html/rfc7231#section-6.5.1)|Bad request. Please check error message and your parameters.|None|
|401|[Unauthorized](https://tools.ietf.org/html/rfc7235#section-3.1)|Authorization failed. Please supply a `Bearer` token via the `Authorization` header.|None|
|403|[Forbidden](https://tools.ietf.org/html/rfc7231#section-6.5.3)|Authorization failed. Please supply a valid `Bearer` token via the `Authorization` header.|None|
|404|[Not Found](https://tools.ietf.org/html/rfc7231#section-6.5.4)|The requested resource could not be found.|None|
|500|[Internal Server Error](https://tools.ietf.org/html/rfc7231#section-6.6.1)|Internal error.|None|

<aside class="warning">
To perform this operation, you must be authenticated by means of one of the following methods:
bearerAuth
</aside>

<details>

<summary>Code samples</summary>

```javascript
const inputBody = '{
  "uuid": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
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
  "uuid": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
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
  "uuid": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
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
  "uuid": "f0ec68aa-63f5-405c-b178-9a4454556d6b",
  "command": "echo $USER"
}
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
    {
      "uuid": "string",
      "category": [
        "string"
      ],
      "connection": [
        "string"
      ],
      "type": "string"
    }
  ]
}

```

<h3>Properties</h3>

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|found|[object]|true|none|The found connections|
|» uuid|string|true|none|The unique id of the connection|
|» category|[string]|true|none|The full category path as an array|
|» connection|[string]|true|none|The full connection name path as an array|
|» type|string|true|none|The type identifier of the connection|

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

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|type|string|true|none|none|

oneOf

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|*anonymous*|[ApiKey](#schemaapikey)|false|none|API key authentication|

xor

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|*anonymous*|[Local](#schemalocal)|false|none|Authentication method for local applications. Uses file system access as proof of authentication.|

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

allOf - discriminator: AuthMethod.type

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|*anonymous*|[AuthMethod](#schemaauthmethod)|false|none|none|

and

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|*anonymous*|object|false|none|none|
|» key|string|true|none|The API key|

<h2 id="tocS_Local">Local</h2>

<a id="schemalocal"></a>
<a id="schema_Local"></a>
<a id="tocSlocal"></a>
<a id="tocslocal"></a>

```json
{
  "type": "string",
  "key": "string",
  "authFileContent": "string"
}

```

Authentication method for local applications. Uses file system access as proof of authentication.

<h3>Properties</h3>

allOf - discriminator: AuthMethod.type

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|*anonymous*|[AuthMethod](#schemaauthmethod)|false|none|none|

and

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|*anonymous*|object|false|none|none|
|» authFileContent|string|true|none|The contents of the local file $TEMP/xpipe_auth. This file is automatically generated when XPipe starts.|

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

