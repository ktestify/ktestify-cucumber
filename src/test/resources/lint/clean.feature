Feature: Clean feature

  Background:
    Given namespace
      | namespace |
      | ktestify  |

    Given input topic
      | topicName | topicAlias | namespace |
      | orders-in | in         | ktestify  |

    Given output topic
      | topicName | topicAlias | namespace |
      | orders-out | out       | ktestify  |

  Scenario: Round trip
    When record from file is sent
      | topicAlias | file                | recordKey |
      | in         | payload.json        | key-001   |

    Then expected record from file
      | topicAlias | file                | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | out        | expected.json       | key-001            | 15                  | 30                |
