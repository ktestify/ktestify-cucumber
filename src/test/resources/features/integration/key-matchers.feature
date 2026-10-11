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

  # ── KeyRecordMatcher — keyMatchStrategy variations ──────────────────────────

  @keyStrategy
  Scenario: KeyRecordMatcher — starts_with strategy matches key prefix
    # Record key "m-key-001" matches expected prefix "m-key-00" via starts_with strategy
    When record from file is sent
      | topicAlias | file                            | recordKey    |
      | rt-in      | integration/m-key-payload.json  | m-key-001    |

    Then expected record key matches
      | topicAlias | expectedRecordKey | keyMatchStrategy | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | m-key-00          | starts_with      | 15                  | 30                |

  @keyStrategy
  Scenario: KeyRecordMatcher — ends_with strategy matches key suffix
    # Record key "m-key-001" matches expected suffix "001" via ends_with strategy
    When record from file is sent
      | topicAlias | file                            | recordKey    |
      | rt-in      | integration/m-key-payload.json  | m-key-001    |

    Then expected record key matches
      | topicAlias | expectedRecordKey | keyMatchStrategy | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | 001               | ends_with        | 15                  | 30                |

  @keyStrategy
  Scenario: KeyRecordMatcher — contains strategy matches key substring
    # Record key "m-key-001" contains substring "key" via contains strategy
    When record from file is sent
      | topicAlias | file                            | recordKey    |
      | rt-in      | integration/m-key-payload.json  | m-key-001    |

    Then expected record key matches
      | topicAlias | expectedRecordKey | keyMatchStrategy | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | key               | contains         | 15                  | 30                |

  @keyStrategy
  Scenario: KeyRecordMatcher — regex strategy matches key pattern
    # Record key "m-key-001" matches regex pattern "m-key-\\d+" via regex strategy
    When record from file is sent
      | topicAlias | file                            | recordKey    |
      | rt-in      | integration/m-key-payload.json  | m-key-001    |

    Then expected record key matches
      | topicAlias | expectedRecordKey   | keyMatchStrategy | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | m-key-\d+           | regex            | 15                  | 30                |

  @keyStrategy
  Scenario: KeyRecordMatcher — starts_with strategy does not match wrong prefix (negative path)
    # Record key "m-key-001" does NOT start with "wrong-" → consumer times out → watcher passes
    When record from file is sent
      | topicAlias | file                            | recordKey    |
      | rt-in      | integration/m-key-payload.json  | m-key-001    |

    And record should not appear in topic
      | topicAlias | topicType | expectedRecordKey | keyMatchStrategy | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | raw       | wrong-             | starts_with      | 5                   | 10                |

  # ── FileKeyRecordMatcher — keyMatchStrategy variations ──────────────────────

  @keyStrategy
  Scenario: FileKeyRecordMatcher — starts_with strategy matches key prefix and value from file
    # Both key prefix "m-kv-001" (starts_with, exact prefix) and value (vs m-kv-expected.json) are asserted.
    # Using the full key as prefix avoids ambiguity with m-kv-002 from the prior scenario.
    When record from file is sent
      | topicAlias | file                          | recordKey  |
      | rt-in      | integration/m-kv-payload.json | m-kv-001   |

    Then expected record key and value match from file
      | topicAlias | file                           | expectedRecordKey | keyMatchStrategy | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/m-kv-expected.json | m-kv-001          | starts_with      | 15                  | 30                |

  @keyStrategy
  Scenario: FileKeyRecordMatcher — contains strategy matches key substring and value from file
    # Both key substring "kv-001" (contains) and value (vs m-kv-expected.json) are asserted.
    # "kv-001" is unique among all keys on the topic, avoiding mismatch with m-kv-decoy / m-kv-002.
    When record from file is sent
      | topicAlias | file                          | recordKey  |
      | rt-in      | integration/m-kv-payload.json | m-kv-001   |

    Then expected record key and value match from file
      | topicAlias | file                           | expectedRecordKey | keyMatchStrategy | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/m-kv-expected.json | kv-001            | contains         | 15                  | 30                |

  @keyStrategy
  Scenario: FileKeyRecordMatcher — regex strategy matches key pattern and value from file
    # Both key regex "m-kv-001" (regex, literal pattern) and value (vs m-kv-expected.json) are asserted.
    # Using a literal regex avoids ambiguity with m-kv-002 from the prior scenario.
    When record from file is sent
      | topicAlias | file                          | recordKey  |
      | rt-in      | integration/m-kv-payload.json | m-kv-001   |

    Then expected record key and value match from file
      | topicAlias | file                           | expectedRecordKey | keyMatchStrategy | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/m-kv-expected.json | m-kv-001          | regex            | 15                  | 30                |
