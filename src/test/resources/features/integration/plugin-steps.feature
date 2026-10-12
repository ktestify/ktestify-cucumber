Feature: First-party plugin step integration

  Background:
    Given HTTP endpoints
      | endpointAlias | baseUrl          |
      | http-main     | http://127.0.0.1:18765/api |
      | http-second   | http://127.0.0.1:18765/api |
    Given HTTP bearer token
      | endpointAlias | token          |
      | http-main     | cucumber-token |
    Given HTTP assets directory
      | absolutePath       |
      | src/test/resources/data/integration/plugins |
    Given Azure Blob Storage containers
      | containerName             | containerAlias |
      | ktestify-plugin-it-json   | azure-json     |
      | ktestify-plugin-it-xml    | azure-xml      |
    Given Azure Blob assets directory
      | absolutePath       |
      | src/test/resources/data/integration/plugins |

  @pluginIntegration @httpPlugin
  Scenario: Send a file request with query parameters and validate a JSON response
    When HTTP request is sent
      | endpointAlias | method | path             | file                 | queryParams  | responseAlias |
      | http-main     | POST   | /orders/validate | http-request.json    | source=it    | http-json     |
    Then expected HTTP response status
      | responseAlias | statusCode |
      | http-json     | 200        |
    And expected HTTP response body from file
      | responseAlias | file               |
      | http-json     | http-expected.json |
    And HTTP response header should match
      | responseAlias | header       | value            |
      | http-json     | Content-Type | application/json |

  @pluginIntegration @httpPlugin
  Scenario: Send an inline request through a singular endpoint and validate its response
    Given HTTP endpoint
      | endpointAlias | baseUrl          |
      | http-inline   | http://127.0.0.1:18765/api |
    When HTTP request is sent
      | endpointAlias | method | path   | body                                | responseAlias |
      | http-inline   | POST   | /echo  | {"message":"inline","id":"HTTP-002"} | http-inline   |
    Then expected HTTP response status
      | responseAlias | statusCode |
      | http-inline  | 200        |
    And expected HTTP response body from file
      | responseAlias | file                    |
      | http-inline   | http-inline-expected.json |

  @pluginIntegration @httpPlugin
  Scenario: Validate an XML response while excluding a volatile element
    When HTTP request is sent
      | endpointAlias | method | path        | responseAlias |
      | http-second   | GET    | /orders/xml | http-xml      |
    Then expected HTTP response XML body from file
      | responseAlias | file              | excludedElements |
      | http-xml      | http-expected.xml | processedAt      |

  @pluginIntegration @httpPlugin
  Scenario: Poll an endpoint until its expected status is returned
    Then HTTP endpoint should eventually return
      | endpointAlias | method | path          | expectedStatus | readTimeout | queryParams |
      | http-main     | GET    | /orders/status | 200            | 5           | order=1     |

  @pluginIntegration @azureBlobPlugin
  Scenario: Upload a blob, assert JSON content, and check existence and absence
    When blob is uploaded from file
      | containerAlias | file                | blobName              |
      | azure-json     | azure-payload.json  | integration/order.json |
    Then expected blob content from file
      | containerAlias | blobName              | file                |
      | azure-json     | integration/order.json | azure-expected.json |
    And blob should exist
      | containerAlias | blobName              | readTimeout |
      | azure-json     | integration/order.json | 5           |
    And blob should not exist
      | containerAlias | blobName                   | readTimeout |
      | azure-json     | integration/missing.json   | 2           |

  @pluginIntegration @azureBlobPlugin
  Scenario: Register one container and compare uploaded XML content
    Given Azure Blob Storage container
      | containerName           | containerAlias |
      | ktestify-plugin-it-xml  | azure-single   |
    When blob is uploaded from file
      | containerAlias | file              | blobName              |
      | azure-single   | azure-payload.xml | integration/order.xml |
    Then expected blob XML content from file
      | containerAlias | blobName              | file                    | excludedElements | readTimeout |
      | azure-single   | integration/order.xml | azure-expected.xml      | ProcessedAt      | 5           |
    And blob should exist
      | containerAlias | blobName              | readTimeout |
      | azure-single   | integration/order.xml | 5           |

  @pluginFailure @httpPluginFailure
  Scenario: Fail when the response status does not match
    When HTTP request is sent
      | endpointAlias | method | path          | responseAlias |
      | http-main     | GET    | /orders/steady | status-fail   |
    Then expected HTTP response status
      | responseAlias | statusCode |
      | status-fail   | 503        |

  @pluginFailure @httpPluginFailure
  Scenario: Fail when the JSON response body does not match
    When HTTP request is sent
      | endpointAlias | method | path             | responseAlias |
      | http-main     | POST   | /orders/validate | body-fail     |
    Then expected HTTP response body from file
      | responseAlias | file             |
      | body-fail     | http-wrong.json  |

  @pluginFailure @httpPluginFailure
  Scenario: Fail when the XML response body does not match
    When HTTP request is sent
      | endpointAlias | method | path        | responseAlias |
      | http-main     | GET    | /orders/xml | xml-fail      |
    Then expected HTTP response XML body from file
      | responseAlias | file            |
      | xml-fail      | http-wrong.xml  |

  @pluginFailure @httpPluginFailure
  Scenario: Fail when the response header does not match
    When HTTP request is sent
      | endpointAlias | method | path          | responseAlias |
      | http-main     | GET    | /orders/steady | header-fail   |
    And HTTP response header should match
      | responseAlias | header       | value          |
      | header-fail   | Content-Type | text/plain     |

  @pluginFailure @httpPluginFailure
  Scenario: Fail when polling never returns the expected status
    Then HTTP endpoint should eventually return
      | endpointAlias | method | path                | expectedStatus | readTimeout |
      | http-main     | GET    | /orders/never-ready | 200            | 1           |

  @pluginFailure @httpPluginFailure
  Scenario: Fail when a request specifies both body input modes
    When HTTP request is sent
      | endpointAlias | method | path  | file              | body | responseAlias |
      | http-main     | POST   | /echo | http-request.json | {}   | invalid-body  |

  @pluginFailure @azureBlobPluginFailure
  Scenario: Fail when uploaded blob JSON content differs from the expected file
    When blob is uploaded from file
      | containerAlias | file               | blobName                    |
      | azure-json     | azure-payload.json | integration/mismatch.json   |
    Then expected blob content from file
      | containerAlias | blobName                    | file                     | readTimeout |
      | azure-json     | integration/mismatch.json  | azure-wrong.json         | 5           |

  @pluginFailure @azureBlobPluginFailure
  Scenario: Fail when uploaded blob XML content differs from the expected file
    When blob is uploaded from file
      | containerAlias | file              | blobName                  |
      | azure-xml      | azure-payload.xml | integration/mismatch.xml |
    Then expected blob XML content from file
      | containerAlias | blobName                  | file                | readTimeout |
      | azure-xml      | integration/mismatch.xml | azure-wrong.xml     | 5           |

  @pluginFailure @azureBlobPluginFailure
  Scenario: Fail when a missing blob is expected to exist
    Then blob should exist
      | containerAlias | blobName                     | readTimeout |
      | azure-json     | integration/never-created   | 2           |

  @pluginFailure @azureBlobPluginFailure
  Scenario: Fail when an existing blob is expected not to exist
    When blob is uploaded from file
      | containerAlias | file               | blobName                  |
      | azure-json     | azure-payload.json | integration/unexpected.json |
    And blob should not exist
      | containerAlias | blobName                   | readTimeout |
      | azure-json     | integration/unexpected.json | 2           |
