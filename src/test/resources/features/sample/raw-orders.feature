@raw
Feature: Raw Kafka record validation — sample tests
  # NOTE: These are self-contained framework tests.
  # We produce and consume from the SAME physical Kafka topic by registering it
  # as both INPUT (for the producer) and OUTPUT (for the consumer).
  # In real usage: input topic → your application → output topic.

  Background:
    Given namespace
      | namespace |
      | ktestify  |

    Given input topic
      | topicName      | topicAlias  | namespace |
      | raw-roundtrip  | raw-in      | ktestify  |

    Given output topic
      | topicName      | topicAlias  | namespace |
      | raw-roundtrip  | raw-out     | ktestify  |

  # -- Scenario 1: exact file match ---------------------------------
  Scenario: Produced raw record matches expected JSON file
    When record from file is sent
      | topicAlias      | file            | recordKey |
      | raw-in  | send-order.json | order-001 |

    Then expected record from file
      | topicAlias | file                | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | raw-out    | expected-order.json | order-001         | 30                  | 60                |

  # -- Scenario 2: XML match ----------------------------------------
  Scenario: Produced raw XML record matches expected XML file
    When record from file is sent
      | topicAlias     | file           | recordKey     |
      | raw-in         | send-order.xml | order-xml-001 |

    Then expected record from file based on XML
      | topicAlias | file               | expectedRecordKey | excludedElements  |
      | raw-out    | expected-order.xml | order-xml-001     | ProcessedDateTime |

    And wait for 5 seconds
  # -- Scenario 3: record must NOT appear (negative assertion) ------
  Scenario: No record appears when nothing is produced
    And record should not appear in topic
      | topicAlias | topicType | consumerReadTimeout | consumerDeltaTime |
      | raw-out    | raw       | 5                   | 1                 |

