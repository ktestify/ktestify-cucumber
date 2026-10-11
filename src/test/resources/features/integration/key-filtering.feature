@integration @key-filtering
Feature: Record key filtering

  # Tests the expectedRecordKey parameter across all edge cases:
  #   - Exact key match
  #   - Key present but wrong value → consumer ignores the record, eventually times out
  #   - No key filter → first record accepted regardless of key
  #   - Multiple records on the topic → only the one with the matching key is returned

  Background:
    Given namespace
      | namespace |
      | ktestify  |

    Given input topic
      | topicName      | topicAlias | namespace |
      | raw-roundtrip  | rt-in      | ktestify  |

    Given output topic
      | topicName      | topicAlias | namespace |
      | raw-roundtrip  | rt-out     | ktestify  |

  # ── exact key match ───────────────────────────────────────────────────────

  @keyMatch
  Scenario: Consumer matches record by exact key
    When record from file is sent
      | topicAlias | file                         | recordKey   |
      | rt-in      | integration/kf-payload-a.json | kf-key-AAA  |

    Then expected record from file
      | topicAlias | file                          | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/kf-expected-a.json | kf-key-AAA        | 15                  | 30                |

    And wait for 5 seconds
  # ── no key filter → first record accepted ─────────────────────────────────

  @keyMatch
  Scenario: Consumer with no key filter accepts first available record
    When record from file is sent
      | topicAlias | file                         | recordKey   |
      | rt-in      | integration/kf-payload-b.json | kf-key-BBB  |

    # No expectedRecordKey column — any record on the topic is accepted
    Then expected record from file
      | topicAlias | file                          | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/kf-expected-b.json | 15                  | 30                |

  # ── multiple records, only one matches key ────────────────────────────────

  @keyMatch
  Scenario: Consumer skips records with wrong key and picks the correct one
    # Produce decoy first, then the real record
    When record from file is sent
      | topicAlias | file                              | recordKey      |
      | rt-in      | integration/kf-payload-decoy.json  | kf-key-DECOY   |

    And record from file is sent
      | topicAlias | file                         | recordKey   |
      | rt-in      | integration/kf-payload-c.json | kf-key-CCC  |

    Then expected record from file
      | topicAlias | file                          | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/kf-expected-c.json | kf-key-CCC        | 15                  | 30                |

  # ── key filter with no matching record → timeout ──────────────────────────

  @keyMatch
  Scenario: Consumer times out when expected key is not produced
    # Produce with key X, filter by key Y → consumer must time out
    When record from file is sent
      | topicAlias | file                         | recordKey   |
      | rt-in      | integration/kf-payload-a.json | kf-key-AAA  |

    And record should not appear in topic
      | topicAlias | topicType | consumerReadTimeout | consumerDeltaTime | expectedRecordKey |
      | rt-out     | raw       | 5                   | 10                | kf-key-NONEXISTENT |

  # ── keyMatchStrategy — non-exact key matching ──────────────────────────────

  @keyStrategy
  Scenario: Consumer matches record by key prefix using starts_with strategy
    When record from file is sent
      | topicAlias | file                         | recordKey   |
      | rt-in      | integration/kf-payload-a.json | kf-key-AAA  |

    Then expected record from file
      | topicAlias | file                          | expectedRecordKey | keyMatchStrategy | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/kf-expected-a.json | kf-key-AA         | starts_with      | 15                  | 30                |

  @keyStrategy
  Scenario: Consumer matches record by key suffix using ends_with strategy
    When record from file is sent
      | topicAlias | file                         | recordKey   |
      | rt-in      | integration/kf-payload-a.json | kf-key-AAA  |

    Then expected record from file
      | topicAlias | file                          | expectedRecordKey | keyMatchStrategy | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/kf-expected-a.json | AAA               | ends_with        | 15                  | 30                |

  @keyStrategy
  Scenario: Consumer matches record by key substring using contains strategy
    # "key-A" is unique to kf-key-AAA among all keys on the topic, avoiding mismatch with
    # kf-key-BBB / kf-key-CCC / kf-key-DECOY from prior scenarios.
    When record from file is sent
      | topicAlias | file                         | recordKey   |
      | rt-in      | integration/kf-payload-a.json | kf-key-AAA  |

    Then expected record from file
      | topicAlias | file                          | expectedRecordKey | keyMatchStrategy | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/kf-expected-a.json | key-A             | contains         | 15                  | 30                |

  @keyStrategy
  Scenario: Consumer matches record by regex pattern using regex strategy
    # "kf-key-A\w+" only matches kf-key-AAA, not kf-key-DECOY/BBB/CCC from prior scenarios
    When record from file is sent
      | topicAlias | file                         | recordKey   |
      | rt-in      | integration/kf-payload-a.json | kf-key-AAA  |

    Then expected record from file
      | topicAlias | file                          | expectedRecordKey | keyMatchStrategy | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/kf-expected-a.json | kf-key-A\w+       | regex            | 15                  | 30                |

  @keyStrategy
  Scenario: Consumer skips records with non-matching key prefix and picks correct one
    # Produce decoy with key "kf-key-DECOY", then real record with "kf-key-CCC"
    # Filter by starts_with "kf-key-C" → only "kf-key-CCC" matches
    When record from file is sent
      | topicAlias | file                              | recordKey      |
      | rt-in      | integration/kf-payload-decoy.json  | kf-key-DECOY   |

    And record from file is sent
      | topicAlias | file                         | recordKey   |
      | rt-in      | integration/kf-payload-c.json | kf-key-CCC  |

    Then expected record from file
      | topicAlias | file                          | expectedRecordKey | keyMatchStrategy | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/kf-expected-c.json | kf-key-C          | starts_with      | 15                  | 30                |
