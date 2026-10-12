Feature: Alias coverage

  Background:
    Given namespace
      | namespace | namespaceAlias |
      | ns-one    | ns-a            |

    Given schema
      | schemaName | schemaAlias | schemaVersion |
      | order-v1   | sch-a       | 1             |

    Given input topic
      | topicName | topicAlias | namespaceAlias |
      | orders-in | in         | ns-a            |

  Scenario: Declared aliases resolve
    When record from file is sent
      | topicAlias | file         | recordKey |
      | in         | payload.json | key-001   |

    Then expected records from files
      | topicAlias | files                 | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | in         | payload.json,extra.json | key-001          | 15                  | 30                |