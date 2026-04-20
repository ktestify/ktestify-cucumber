@integration @timeout
Feature: Timeout and delta-time parameter validation

  # Tests the two timeout dimensions:
  #   consumerReadTimeout — how long the inner Kafka poller waits (seconds in DataTable → ms internally)
  #   consumerDeltaTime   — how far back in time to seek before polling (seconds in DataTable → ms internally)
  #
  # All scenarios use the roundtrip pattern: produce to raw-roundtrip (INPUT),
  # consume from raw-roundtrip (OUTPUT), same physical Kafka topic.

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

  # ── readTimeout ───────────────────────────────────────────────────────────

  @readTimeout
  Scenario: Consumer finds record within a short read timeout (5 s)
    # Verifies the inner poller succeeds well within the timeout.
    When record from file is sent
      | topicAlias | file                         | recordKey       |
      | rt-in      | integration/rt-payload-1.json | rt-timeout-001  |

    Then expected record from file
      | topicAlias | file                           | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/rt-expected-1.json  | rt-timeout-001    | 5                   | 30                |

  @readTimeout
  Scenario: Consumer times out when no record is produced (negative path)
    # No produce step and a key that will never be produced — consumer must time out.
    And record should not appear in topic
      | topicAlias | topicType | expectedRecordKey    | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | raw       | rt-never-produced    | 5                   | 10                |

  @readTimeout
  Scenario: Consumer finds record with a generous read timeout (30 s)
    # Verifies that a large timeout value is honoured as seconds, not milliseconds.
    When record from file is sent
      | topicAlias | file                         | recordKey       |
      | rt-in      | integration/rt-payload-2.json | rt-timeout-002  |

    Then expected record from file
      | topicAlias | file                           | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/rt-expected-2.json  | rt-timeout-002    | 30                  | 60                |

  # ── consumerDeltaTime ─────────────────────────────────────────────────────

  @deltaTime
  Scenario: Consumer respects a small delta time (10 s look-back)
    # Produce then immediately consume — delta of 10 s is enough to catch a just-produced record.
    When record from file is sent
      | topicAlias | file                         | recordKey      |
      | rt-in      | integration/rt-payload-3.json | rt-delta-001   |

    Then expected record from file
      | topicAlias | file                           | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/rt-expected-3.json  | rt-delta-001      | 15                  | 10                |

  @deltaTime
  Scenario: Consumer uses a large delta time (120 s look-back)
    # Tests that large delta values are also handled correctly.
    When record from file is sent
      | topicAlias | file                         | recordKey      |
      | rt-in      | integration/rt-payload-4.json | rt-delta-002   |

    Then expected record from file
      | topicAlias | file                           | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/rt-expected-4.json  | rt-delta-002      | 15                  | 120               |

  @deltaTime
  Scenario: Consumer with no explicit timeouts falls back to config defaults
    # No consumerReadTimeout or consumerDeltaTime columns — must use local.conf defaults (30 s / 60 s).
    When record from file is sent
      | topicAlias | file                         | recordKey       |
      | rt-in      | integration/rt-payload-5.json | rt-default-001  |

    Then expected record from file
      | topicAlias | file                           | expectedRecordKey |
      | rt-out     | integration/rt-expected-5.json  | rt-default-001    |

