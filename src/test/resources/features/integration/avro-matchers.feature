@integration @avroMatchers
Feature: Avro record matcher coverage

  # Tests every available Avro RecordMatcher implementation through ConsumerValidationService:
  #   - AvroFileRecordMatcher   (validateAvroFromFile — no exclusions)
  #   - AvroFileRecordMatcher   (validateAvroFromFile — with excludedKeys)
  #   - AvroFieldsRecordMatcher (validateAvroFieldValue — inline key/value assertion)
  #   - AvroKeyRecordMatcher    (validateAvroKeyOnly — key-only assertion)
  #   - Batch Avro              (validateAvroBatch — two records by index)
  #
  # All scenarios use the roundtrip pattern:
  # same physical topic (avro-roundtrip) registered as INPUT and OUTPUT.

  Background:
    Given namespace
      | namespace |
      | ktestify  |

    Given input topic
      | topicName   | topicAlias | namespace |
      | avro-orders | avro-in    | ktestify  |

    Given output topic
      | topicName   | topicAlias | namespace |
      | avro-orders | avro-out   | ktestify  |

    Given schema
      | schemaName | schemaAlias | schemaVersion |
      | Order      | order       | 1             |

  # ── AvroFileRecordMatcher — no field exclusions ──────────────────────────

  @avroFileMatch
  Scenario: Avro record matches expected JSON file — no exclusions
    When record from file based on schema is sent
      | topicAlias | file                 | schemaName | recordKey     |
      | avro-in    | send-order-avro.json | Order      | avro-file-001 |

    Then expected record from file based on schema
      | topicAlias | file                     | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | avro-out   | expected-order-avro.json | avro-file-001     | 20                  | 60                |

  # ── AvroFileRecordMatcher — with excludedKeys ────────────────────────────

  @avroFileMatch
  Scenario: Avro record matches expected JSON file — createdAt excluded
    # The Order schema has a nullable createdAt epoch field.
    # Excluding it lets the comparison pass even when the broker sets it at produce time.
    When record from file based on schema is sent
      | topicAlias | file                 | schemaName | recordKey     |
      | avro-in    | send-order-avro.json | Order      | avro-excl-001 |

    Then expected record from file based on schema
      | topicAlias | file                     | expectedRecordKey | excludedKeys | consumerReadTimeout | consumerDeltaTime |
      | avro-out   | expected-order-avro.json | avro-excl-001     | createdAt    | 20                  | 60                |

  # ── AvroFieldsRecordMatcher — inline key/value ───────────────────────────

  @avroFieldValue
  Scenario: Avro record has expected status value
    When record from file based on schema is sent
      | topicAlias | file                 | schemaName | recordKey      |
      | avro-in    | send-order-avro.json | Order      | avro-field-001 |

    Then expected record based on schema should have fields matching from given value
      | topicAlias | key    | value   | consumerReadTimeout | consumerDeltaTime |
      | avro-out   | status | PENDING | 20                  | 60                |

  @avroFieldValue
  Scenario: Avro record has expected currency value
    When record from file based on schema is sent
      | topicAlias | file                 | schemaName | recordKey      |
      | avro-in    | send-order-avro.json | Order      | avro-field-002 |

    Then expected record based on schema should have fields matching from given value
      | topicAlias | key      | value | consumerReadTimeout | consumerDeltaTime |
      | avro-out   | currency | USD   | 20                  | 60                |

  # ── AvroKeyRecordMatcher — key-only assertion ─────────────────────────────

  @avroKeyMatch
  Scenario: Avro record key matches expected key
    When record from file based on schema is sent
      | topicAlias | file                 | schemaName | recordKey    |
      | avro-in    | send-order-avro.json | Order      | avro-key-001 |

    Then expected Avro record key matches
      | topicAlias | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | avro-out   | avro-key-001      | 20                  | 60                |

  # ── validateAvroBatch — two records matched by index ─────────────────────

  @avroBatch
  Scenario: Batch of 2 Avro records matched in order by index
    When record from file based on schema is sent
      | topicAlias | file                            | schemaName | recordKey      |
      | avro-in    | integration/avro-payload-1.json | Order      | avro-batch-001 |

    And record from file based on schema is sent
      | topicAlias | file                            | schemaName | recordKey      |
      | avro-in    | integration/avro-payload-2.json | Order      | avro-batch-002 |

    Then expected records from files based on schema
      | topicAlias | expectedRecordsCount | files                                                             | excludedKeys | consumerReadTimeout | consumerDeltaTime |
      | avro-out   | 2                    | integration/avro-expected-1.json,integration/avro-expected-2.json | createdAt    | 30                  | 60                |

  @avroBatch
  Scenario: Avro batch — createdAt excluded from each record comparison
    # Producer may stamp createdAt at runtime; exclude it so only business fields are compared.
    When record from file based on schema is sent
      | topicAlias | file                            | schemaName | recordKey      |
      | avro-in    | integration/avro-payload-1.json | Order      | avro-batch-003 |

    And record from file based on schema is sent
      | topicAlias | file                            | schemaName | recordKey      |
      | avro-in    | integration/avro-payload-2.json | Order      | avro-batch-004 |

    Then expected records from files based on schema
      | topicAlias | expectedRecordsCount | files                                                             | excludedKeys | consumerReadTimeout | consumerDeltaTime |
      | avro-out   | 2                    | integration/avro-expected-1.json,integration/avro-expected-2.json | createdAt    | 30                  | 60                |


