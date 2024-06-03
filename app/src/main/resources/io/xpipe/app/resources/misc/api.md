---
title: XPipe API Documentation v10.0
language_tabs:
  - shell: Shell
  - http: HTTP
  - javascript: JavaScript
  - ruby: Ruby
  - python: Python
  - php: PHP
  - java: Java
  - go: Go
toc_footers:
  - <a href="https://xpipe.io/pricing">XPipe - Plans and pricing</a>
includes: []
search: true
highlight_theme: darkula
headingLevel: 2

---

<h1 id="xpipe-api-documentation">XPipe API Documentation v10.0</h1>

[TOC]

> Scroll down for code samples, example requests and responses. Select a language for code samples from the tabs above or the mobile navigation menu.

The XPipe API provides programmatic access to XPipe’s features.

Base URLs:

* <a href="https://localhost:21721">https://localhost:21721</a>

* <a href="https://localhost:21722">https://localhost:21722</a>

undefined

<h1 id="xpipe-api-documentation-default">Default</h1>

## Create new session

<a id="opIdhandshake"></a>

`POST /handshake`

Creates a new API session, allowing you to send requests to the daemon once it is established.

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

undefined

undefined

undefined

undefined

> Code samples

```shell
# You can also use wget
curl -X POST https://localhost:21721/handshake \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json'

```

```http
POST https://localhost:21721/handshake HTTP/1.1
Host: localhost:21721
Content-Type: application/json
Accept: application/json

```

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

fetch('https://localhost:21721/handshake',
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

```ruby
require 'rest-client'
require 'json'

headers = {
  'Content-Type' => 'application/json',
  'Accept' => 'application/json'
}

result = RestClient.post 'https://localhost:21721/handshake',
  params: {
  }, headers: headers

p JSON.parse(result)

```

```python
import requests
headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
}

r = requests.post('https://localhost:21721/handshake', headers = headers)

print(r.json())

```

```php
<?php

require 'vendor/autoload.php';

$headers = array(
    'Content-Type' => 'application/json',
    'Accept' => 'application/json',
);

$client = new \GuzzleHttp\Client();

// Define array of request body.
$request_body = array();

try {
    $response = $client->request('POST','https://localhost:21721/handshake', array(
        'headers' => $headers,
        'json' => $request_body,
       )
    );
    print_r($response->getBody()->getContents());
 }
 catch (\GuzzleHttp\Exception\BadResponseException $e) {
    // handle exception or api errors.
    print_r($e->getMessage());
 }

 // ...

```

```java
URL obj = new URL("https://localhost:21721/handshake");
HttpURLConnection con = (HttpURLConnection) obj.openConnection();
con.setRequestMethod("POST");
int responseCode = con.getResponseCode();
BufferedReader in = new BufferedReader(
    new InputStreamReader(con.getInputStream()));
String inputLine;
StringBuffer response = new StringBuffer();
while ((inputLine = in.readLine()) != null) {
    response.append(inputLine);
}
in.close();
System.out.println(response.toString());

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
    req, err := http.NewRequest("POST", "https://localhost:21721/handshake", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

## Query connections

<a id="opIdconnectionQuery"></a>

`POST /connection/query`

Queries all connections using various filters

> Body parameter

```json
{
  "categoryFilter": "**",
  "connectionFilter": "**",
  "typeFilter": "*"
}
```

undefined

undefined

undefined

undefined

> Code samples

```shell
# You can also use wget
curl -X POST https://localhost:21721/connection/query \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json'

```

```http
POST https://localhost:21721/connection/query HTTP/1.1
Host: localhost:21721
Content-Type: application/json
Accept: application/json

```

```javascript
const inputBody = '{
  "categoryFilter": "**",
  "connectionFilter": "**",
  "typeFilter": "*"
}';
const headers = {
  'Content-Type':'application/json',
  'Accept':'application/json'
};

fetch('https://localhost:21721/connection/query',
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

```ruby
require 'rest-client'
require 'json'

headers = {
  'Content-Type' => 'application/json',
  'Accept' => 'application/json'
}

result = RestClient.post 'https://localhost:21721/connection/query',
  params: {
  }, headers: headers

p JSON.parse(result)

```

```python
import requests
headers = {
  'Content-Type': 'application/json',
  'Accept': 'application/json'
}

r = requests.post('https://localhost:21721/connection/query', headers = headers)

print(r.json())

```

```php
<?php

require 'vendor/autoload.php';

$headers = array(
    'Content-Type' => 'application/json',
    'Accept' => 'application/json',
);

$client = new \GuzzleHttp\Client();

// Define array of request body.
$request_body = array();

try {
    $response = $client->request('POST','https://localhost:21721/connection/query', array(
        'headers' => $headers,
        'json' => $request_body,
       )
    );
    print_r($response->getBody()->getContents());
 }
 catch (\GuzzleHttp\Exception\BadResponseException $e) {
    // handle exception or api errors.
    print_r($e->getMessage());
 }

 // ...

```

```java
URL obj = new URL("https://localhost:21721/connection/query");
HttpURLConnection con = (HttpURLConnection) obj.openConnection();
con.setRequestMethod("POST");
int responseCode = con.getResponseCode();
BufferedReader in = new BufferedReader(
    new InputStreamReader(con.getInputStream()));
String inputLine;
StringBuffer response = new StringBuffer();
while ((inputLine = in.readLine()) != null) {
    response.append(inputLine);
}
in.close();
System.out.println(response.toString());

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
    req, err := http.NewRequest("POST", "https://localhost:21721/connection/query", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

## Open URLs

<a id="opIddaemonOpen"></a>

`POST /daemon/open`

Opens main window or executes given actions.

> Body parameter

```json
{
  "arguments": [
    "file:///home/user/.ssh/"
  ]
}
```

undefined

undefined

undefined

undefined

> Code samples

```shell
# You can also use wget
curl -X POST https://localhost:21721/daemon/open \
  -H 'Content-Type: application/json'

```

```http
POST https://localhost:21721/daemon/open HTTP/1.1
Host: localhost:21721
Content-Type: application/json

```

```javascript
const inputBody = '{
  "arguments": [
    "file:///home/user/.ssh/"
  ]
}';
const headers = {
  'Content-Type':'application/json'
};

fetch('https://localhost:21721/daemon/open',
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

```ruby
require 'rest-client'
require 'json'

headers = {
  'Content-Type' => 'application/json'
}

result = RestClient.post 'https://localhost:21721/daemon/open',
  params: {
  }, headers: headers

p JSON.parse(result)

```

```python
import requests
headers = {
  'Content-Type': 'application/json'
}

r = requests.post('https://localhost:21721/daemon/open', headers = headers)

print(r.json())

```

```php
<?php

require 'vendor/autoload.php';

$headers = array(
    'Content-Type' => 'application/json',
);

$client = new \GuzzleHttp\Client();

// Define array of request body.
$request_body = array();

try {
    $response = $client->request('POST','https://localhost:21721/daemon/open', array(
        'headers' => $headers,
        'json' => $request_body,
       )
    );
    print_r($response->getBody()->getContents());
 }
 catch (\GuzzleHttp\Exception\BadResponseException $e) {
    // handle exception or api errors.
    print_r($e->getMessage());
 }

 // ...

```

```java
URL obj = new URL("https://localhost:21721/daemon/open");
HttpURLConnection con = (HttpURLConnection) obj.openConnection();
con.setRequestMethod("POST");
int responseCode = con.getResponseCode();
BufferedReader in = new BufferedReader(
    new InputStreamReader(con.getInputStream()));
String inputLine;
StringBuffer response = new StringBuffer();
while ((inputLine = in.readLine()) != null) {
    response.append(inputLine);
}
in.close();
System.out.println(response.toString());

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
    }

    data := bytes.NewBuffer([]byte{jsonReq})
    req, err := http.NewRequest("POST", "https://localhost:21721/daemon/open", data)
    req.Header = headers

    client := &http.Client{}
    resp, err := client.Do(req)
    // ...
}

```

# Schemas

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

### Properties

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|categoryFilter|string|true|none|The filter string to match categories. Categories are delimited by / if they are hierarchical. The filter supports globs with * and **.|
|connectionFilter|string|true|none|The filter string to match connection names. Connection names are delimited by / if they are hierarchical. The filter supports globs with * and **.|
|typeFilter|string|true|none|The filter string to connection types. Every unique type of connection like SSH or docker has its own type identifier that you can match. The filter supports globs with *.|

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
      "category": "string",
      "connection": "string",
      "type": "string"
    }
  ]
}

```

### Properties

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|found|[object]|true|none|The found connections|
|» uuid|string|true|none|The unique id of the connection|
|» category|string|true|none|The full category path|
|» connection|string|true|none|The full connection name path|
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

### Properties

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
  "token": "string"
}

```

### Properties

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|token|string|true|none|The generated bearer token that can be used for authentication in this session|

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

### Properties

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

### Properties

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

### Properties

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

### Properties

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

### Properties

allOf - discriminator: ClientInformation.type

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|*anonymous*|[ClientInformation](#schemaclientinformation)|false|none|none|

and

|Name|Type|Required|Restrictions|Description|
|---|---|---|---|---|
|*anonymous*|object|false|none|none|
|» name|string|true|none|The name of the client.|

undefined

