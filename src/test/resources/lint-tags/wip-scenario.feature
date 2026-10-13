Feature: Domain violations

  Background:
    Given namespace
      | namespace |
      | ktestify  |

    Given input topic
      | topicName | topicAlias | namespace |
      | orders-in | in         | ktestify  |

  @WIP
  Scenario: Undeclared alias and missing file
    When record from file is sent
      | topicAlias | file                   | recordKey |
      | unknown    | my payload.json        | key-001   |

    Then expected record from file
      | topicAlias | file         | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | out        | missing.json | key-001           | 15                  | 30                |