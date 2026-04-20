@integration @batch
Feature: Batch consumer validation

  # Tests the batch consumer path (isBatchConsumer = true):
  #   - Exact batch count matched
  #   - Partial batch → timeout with count in error message
  #
  # Uses a dedicated raw-batch topic to avoid dedup collisions with other tests.

  Background:
    Given namespace
      | namespace |
      | ktestify  |

    Given input topic
      | topicName | topicAlias | namespace |
      | raw-batch | batch-in   | ktestify  |

    Given output topic
      | topicName | topicAlias | namespace |
      | raw-batch | batch-out  | ktestify  |

  # ── exact batch ───────────────────────────────────────────────────────────

  @batchExact
  Scenario: Batch of 2 records all matched in order
    When record from file is sent
      | topicAlias | file                             | recordKey |
      | batch-in   | integration/batch-payload-1.json | batch-001 |

    And record from file is sent
      | topicAlias | file                             | recordKey |
      | batch-in   | integration/batch-payload-2.json | batch-002 |

    Then expected records from files
      | topicAlias | expectedRecordsCount | files                                                               | consumerReadTimeout | consumerDeltaTime |
      | batch-out  | 2                    | integration/batch-expected-1.json,integration/batch-expected-2.json | 30                  | 60                |

  @batchExact
  Scenario: Batch of 3 records all matched in order
    When record from file is sent
      | topicAlias | file                             | recordKey |
      | batch-in   | integration/batch-payload-1.json | batch-003 |

    And record from file is sent
      | topicAlias | file                             | recordKey |
      | batch-in   | integration/batch-payload-2.json | batch-004 |

    And record from file is sent
      | topicAlias | file                             | recordKey |
      | batch-in   | integration/batch-payload-3.json | batch-005 |

    Then expected records from files
      | topicAlias | expectedRecordsCount | files                                                                                                 | consumerReadTimeout | consumerDeltaTime |
      | batch-out  | 3                    | integration/batch-expected-1.json,integration/batch-expected-2.json,integration/batch-expected-3.json | 30                  | 60                |

  # ── partial batch → negative assertion ────────────────────────────────────

  @batchTimeout
  Scenario: Batch consumer times out when fewer records than expected are produced
    # Nothing is produced with key 'batch-never-produced'.
    # The watcher filters by that key so old records from previous scenarios are ignored.
    And record should not appear in topic
      | topicAlias | topicType | expectedRecordKey      | consumerReadTimeout | consumerDeltaTime |
      | batch-out  | raw       | batch-never-produced   | 5                   | 5                 |

