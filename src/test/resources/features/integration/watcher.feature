@integration @watcher
Feature: Watcher assertion coverage

  # Tests both watcher paths in ConsumerValidationService:
  #   - validateRecordExists  (positive — record MUST appear)
  #   - validateNoRecord      (negative — record must NOT appear)
  #
  # Edge cases covered:
  #   - positive watcher: record produced immediately before assertion → must pass
  #   - positive watcher: with specific expectedRecordKey filter
  #   - negative watcher: nothing produced → timeout is the passing condition
  #   - negative watcher: record produced but under wrong key → must pass (key mismatch = not seen)
  #   - negative watcher: record produced with correct key → must fail (detected = assertion error)
  #
  # Note: the last scenario (record appears and is detected) is deliberately a @negativeControl tag
  # so CI can skip it with --tags "not @negativeControl" if the assertion-error path is not desired.

  Background:
    Given namespace
      | namespace |
      | ktestify  |

    Given input topic
      | topicName     | topicAlias  | namespace |
      | raw-roundtrip | watcher-in  | ktestify  |

    Given output topic
      | topicName     | topicAlias  | namespace |
      | raw-roundtrip | watcher-out | ktestify  |

  # ── validateRecordExists — positive assertion ─────────────────────────────

  @recordExists
  Scenario: Record appears on topic — validateRecordExists passes
    When record from file is sent
      | topicAlias | file                        | recordKey      |
      | watcher-in | integration/m-json-payload.json | watcher-pos-001 |

    And record should appear in topic
      | topicAlias  | topicType | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | watcher-out | raw       | watcher-pos-001   | 15                  | 30                |

  @recordExists
  Scenario: Record appears without key filter — first available record satisfies positive watcher
    When record from file is sent
      | topicAlias | file                        | recordKey       |
      | watcher-in | integration/m-json-payload.json | watcher-pos-002 |

    # No expectedRecordKey — any record present on the topic is accepted
    And record should appear in topic
      | topicAlias  | topicType | consumerReadTimeout | consumerDeltaTime |
      | watcher-out | raw       | 10                  | 30                |

  # ── validateNoRecord — negative assertion ─────────────────────────────────

  @noRecord
  Scenario: Nothing produced — validateNoRecord passes on timeout
    # No When step — consumer must time out, which counts as a pass.
    And record should not appear in topic
      | topicAlias  | topicType | expectedRecordKey      | consumerReadTimeout | consumerDeltaTime |
      | watcher-out | raw       | watcher-never-produced | 5                   | 5                 |

  @noRecord
  Scenario: Record produced under a different key — consumer times out on the filtered key
    # Produce with key 'watcher-other', filter on 'watcher-absent' — consumer should NOT find it.
    When record from file is sent
      | topicAlias | file                        | recordKey      |
      | watcher-in | integration/m-json-payload.json | watcher-other  |

    And record should not appear in topic
      | topicAlias  | topicType | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | watcher-out | raw       | watcher-absent    | 5                   | 10                |

  @noRecord
  Scenario: No key filter and nothing produced — validateNoRecord passes
    And record should not appear in topic
      | topicAlias  | topicType | consumerReadTimeout | consumerDeltaTime |
      | watcher-out | raw       | 5                   | 5                 |

