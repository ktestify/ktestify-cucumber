@integration @keyMatchers
Feature: Key-based record matcher coverage

  # Tests the two key-oriented raw RecordMatcher implementations that were previously
  # uncovered in ConsumerValidationService:
  #   - KeyRecordMatcher      (methodRecordKeyMatch) — validateRawKeyOnly
  #     Asserts only the record KEY without inspecting the value at all.
  #   - FileKeyRecordMatcher  (methodMatchKeyValue)  — validateRawKeyValue
  #     Asserts both KEY (exact string equality) AND VALUE (vs expected file).
  #
  # Note: the @fileKeyMatch scenario in matchers.feature uses validateRawFromFile
  # (FileRecordMatcher / methodMatchFile) — that is value-only matching. The steps
  # in THIS feature exercise the distinct key-aware matchers.

  Background:
    Given namespace
      | namespace |
      | ktestify  |

    Given input topic
      | topicName     | topicAlias | namespace |
      | raw-roundtrip | rt-in      | ktestify  |

    Given output topic
      | topicName     | topicAlias | namespace |
      | raw-roundtrip | rt-out     | ktestify  |

  # ── KeyRecordMatcher ──────────────────────────────────────────────────────

  @keyOnly
  Scenario: KeyRecordMatcher — record key matches expected key exactly
    # Only the KEY is asserted; the value is not inspected.
    When record from file is sent
      | topicAlias | file                            | recordKey    |
      | rt-in      | integration/m-key-payload.json  | m-key-001    |

    Then expected record key matches
      | topicAlias | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | m-key-001         | 15                  | 30                |

  @keyOnly
  Scenario: KeyRecordMatcher — two consecutive records each validated by key only
    When record from file is sent
      | topicAlias | file                            | recordKey    |
      | rt-in      | integration/m-key-payload.json  | m-key-002    |

    Then expected record key matches
      | topicAlias | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | m-key-002         | 15                  | 30                |

    When record from file is sent
      | topicAlias | file                           | recordKey    |
      | rt-in      | integration/m-json-payload.json | m-key-003    |

    Then expected record key matches
      | topicAlias | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | m-key-003         | 15                  | 30                |

  # ── FileKeyRecordMatcher ──────────────────────────────────────────────────

  @keyValueFile
  Scenario: FileKeyRecordMatcher — key and value both match
    # Both the record KEY ("m-kv-001") and the record VALUE (vs m-kv-expected.json) are asserted.
    When record from file is sent
      | topicAlias | file                          | recordKey  |
      | rt-in      | integration/m-kv-payload.json | m-kv-001   |

    Then expected record key and value match from file
      | topicAlias | file                           | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/m-kv-expected.json | m-kv-001          | 15                  | 30                |

  @keyValueFile
  Scenario: FileKeyRecordMatcher — different key isolates the correct record among others on the topic
    # Produce a decoy record first, then the target. Consumer filters by key before asserting.
    When record from file is sent
      | topicAlias | file                          | recordKey       |
      | rt-in      | integration/m-json-payload.json | m-kv-decoy     |

    And record from file is sent
      | topicAlias | file                          | recordKey  |
      | rt-in      | integration/m-kv-payload.json | m-kv-002   |

    Then expected record key and value match from file
      | topicAlias | file                           | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/m-kv-expected.json | m-kv-002          | 15                  | 30                |

  # ── KeyRecordMatcher — absence (negative guard) ───────────────────────────

  @keyOnly
  Scenario: No record with the filtered key — consumer times out (negative path)
    # Produce with key 'm-key-other'; filter for 'm-key-absent' → must time out → watcher passes.
    When record from file is sent
      | topicAlias | file                           | recordKey     |
      | rt-in      | integration/m-key-payload.json | m-key-other   |

    And record should not appear in topic
      | topicAlias | topicType | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | raw       | m-key-absent      | 5                   | 10                |

