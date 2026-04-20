@avro
Feature: Avro Kafka record validation — sample tests
  # NOTE: Self-contained framework tests using a single roundtrip topic.
  # Same physical topic registered as INPUT and OUTPUT.

  Background:
    Given namespace
      | namespace |
      | ktestify  |

    Given input topic
      | topicName      | topicAlias | namespace |
      | avro-roundtrip | avro-in    | ktestify  |

    Given output topic
      | topicName      | topicAlias | namespace |
      | avro-roundtrip | avro-out   | ktestify  |

    Given schema
      | schemaName | schemaAlias | schemaVersion |
      | Order      | order       | 1             |


  # -- Scenario 1: Avro exact match with field exclusions -----------
  Scenario: Produced Avro record matches expected JSON (excluding volatile timestamp)
    When record from file based on schema is sent
      | topicAlias      | file                 | schemaName | recordKey |
      | avro-in  | send-order-avro.json | Order      | order-002 |

    Then expected record from file based on schema
      | topicAlias | file                     | excludedKeys |
      | avro-out   | expected-order-avro.json | processedAt  |
    And wait for 15 seconds

  # -- Scenario 2: Assert a specific field value -------------------
  Scenario: Produced Avro record has expected status
    When record from file based on schema is sent
      | topicAlias      | file                 | schemaName | recordKey |
      | avro-in | send-order-avro.json | Order      | order-003 |

    Then expected record based on schema should have fields matching from given value
      | topicAlias | key    | value   |
      | avro-out   | status | PENDING |
