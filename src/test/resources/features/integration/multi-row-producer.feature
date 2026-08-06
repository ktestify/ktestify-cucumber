@integration @multiRowProducer
Feature: Multi-row DataTable support for producer and consumer steps (issue #38)

  # Prior to the fix, only the first DataTable row of a producer or assertion step was used — every
  # other row was silently ignored.
  #
  # Producer steps are stateless per-send (no offset/seek math), so multi-row support is unconditionally
  # safe there: every row is sent, in row order, to a single physical topic.
  #
  # Single-record consumer/assertion steps (expected record from file, watchers, key-only assertions, …)
  # now also support multiple rows, but ONLY when every row targets the SAME physical topic. All rows in
  # the same step share one pinned "now" (referenceTimestamp) so their delta-time seek windows are
  # identical instead of drifting row-to-row — this addresses the offset-skew concern from the original
  # issue discussion. Rows are validated sequentially, in order.
  #
  # A same-topic guard rail (io.github.ktestify.utils.TopicUtils#assertSingleTopic) rejects DataTables
  # that mix topics before any producer/consumer call happens — that scenario needs no live Kafka broker
  # so it is covered by JVM-only unit tests (ActionStepDefinitionMultiRowGuardTest,
  # ValidationStepDefinitionMultiRowGuardTest) instead of a feature file here.

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

  @multiRowSend
  Scenario: All rows of a multi-row producer DataTable are sent, in order, to the same topic
    When record from file is sent
      | topicAlias | file                              | recordKey |
      | rt-in      | integration/mrp-payload-1.json    | mrp-001   |
      | rt-in      | integration/mrp-payload-2.json    | mrp-002   |

    Then expected records from files
      | topicAlias | expectedRecordsCount  | files                                                             | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | 2                     | integration/mrp-expected-1.json,integration/mrp-expected-2.json   | 5                   | 5                 |

  @multiRowConsume
  Scenario: A multi-row assertion DataTable validates every row, in order, against the same topic
    When record from file is sent
      | topicAlias | file                              | recordKey |
      | rt-in      | integration/mrp-payload-1.json    | mrp-003   |
      | rt-in      | integration/mrp-payload-2.json    | mrp-004   |

    Then expected record from file
      | topicAlias | file                              | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/mrp-expected-1.json   | mrp-003           | 5                   | 5                 |
      | rt-out     | integration/mrp-expected-2.json   | mrp-004           | 5                   | 5                 |

