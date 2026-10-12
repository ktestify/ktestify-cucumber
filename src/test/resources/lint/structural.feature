Feature: Structural violations

	Background:
    Given namespace
      | namespace |
      | ktestify  |

  Scenario: Tab indentation
    When record from file is sent
      | topicAlias | file         | recordKey |
      | in         | payload.json | key-001   |