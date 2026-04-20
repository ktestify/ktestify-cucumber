@integration @matchers
Feature: Record matcher coverage

  # Tests every available raw RecordMatcher implementation:
  #   - FileRecordMatcher        (methodMatchFile)     — exact JSON/text comparison
  #   - FileKeyRecordMatcher     (methodMatchKeyValue) — key + value vs file
  #   - FieldsRecordMatcher      (methodFieldsToMatch) — positional field extraction
  #   - XmlRecordMatcher         (methodMatchXML)      — XML structural compare + exclusions
  #   - XPathRecordMatcher       (methodMatchXPath)    — XPath expression matching

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

  # ── FileRecordMatcher ─────────────────────────────────────────────────────

  @fileMatch
  Scenario: Exact JSON file match
    When record from file is sent
      | topicAlias | file                          | recordKey    |
      | rt-in      | integration/m-json-payload.json | m-json-001   |

    Then expected record from file
      | topicAlias | file                           | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/m-json-expected.json | m-json-001        | 15                  | 30                |

  # ── FileKeyRecordMatcher ──────────────────────────────────────────────────

  @fileKeyMatch
  Scenario: Key and value both match the expected file
    When record from file is sent
      | topicAlias | file                             | recordKey    |
      | rt-in      | integration/m-keyval-payload.json | m-keyval-001 |

    Then expected record from file
      | topicAlias | file                              | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/m-keyval-expected.json | m-keyval-001      | 15                  | 30                |

  # ── FieldsRecordMatcher (positional) ─────────────────────────────────────

  @fieldsMatch
  Scenario: Positional field extraction — line 1, characters 0 to 8 (orderId value)
    # Record value: {"orderId":"FIELD-001","amount":55.0}
    # Line 1, chars 0-8: {"order  (first 9 chars of the line)
    When record from file is sent
      | topicAlias | file                             | recordKey     |
      | rt-in      | integration/m-fields-payload.txt  | m-fields-001  |

    Then expected record should have fields matching from file
      | topicAlias | file                             | expectedRecordKey | line | from | to | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/m-fields-expected.txt | m-fields-001      | 0    | 0   | 8 | 15                  | 30                |

  # ── XmlRecordMatcher ─────────────────────────────────────────────────────

  @xmlMatch
  Scenario: XML structural match without exclusions
    When record from file is sent
      | topicAlias | file                          | recordKey   |
      | rt-in      | integration/m-xml-payload.xml   | m-xml-001   |

    Then expected record from file based on XML
      | topicAlias | file                           | expectedRecordKey | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/m-xml-expected.xml  | m-xml-001         | 15                  | 30                |
    And clear known messages
  @xmlMatch
  Scenario: XML structural match with one excluded element
    When record from file is sent
      | topicAlias | file                                  | recordKey     |
      | rt-in      | integration/m-xml-volatile-payload.xml  | m-xml-vol-001 |

    Then expected record from file based on XML
      | topicAlias | file                                   | expectedRecordKey | excludedElements | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/m-xml-volatile-expected.xml | m-xml-vol-001     | ProcessedAt      | 15                  | 30                |

  @xmlMatch
  Scenario: XML structural match with multiple excluded elements
    When record from file is sent
      | topicAlias | file                                  | recordKey     |
      | rt-in      | integration/m-xml-volatile-payload.xml  | m-xml-vol-002 |

    Then expected record from file based on XML
      | topicAlias | file                                   | expectedRecordKey | excludedElements           | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/m-xml-volatile-expected.xml | m-xml-vol-002     | ProcessedAt,AuditTraceId   | 15                  | 30                |

  # ── XPathRecordMatcher ────────────────────────────────────────────────────

  @xpathMatch
  Scenario: XPath expression extracts and validates a single field
    When record from file is sent
      | topicAlias | file                          | recordKey     |
      | rt-in      | integration/m-xml-payload.xml   | m-xpath-001   |

    Then expected record based on XML should have fields matching from file
      | topicAlias | file                                    | expectedRecordKey | xpathExpressions                | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/m-xml-payload.xml      | m-xpath-001       | //Status/text()                 | 15                  | 30                |

  @xpathMatch
  Scenario: Multiple XPath expressions all match
    When record from file is sent
      | topicAlias | file                          | recordKey     |
      | rt-in      | integration/m-xml-payload.xml   | m-xpath-002   |

    Then expected record based on XML should have fields matching from file
      | topicAlias | file                                    | expectedRecordKey | xpathExpressions                               | consumerReadTimeout | consumerDeltaTime |
      | rt-out     | integration/m-xml-payload.xml      | m-xpath-002       | //Status/text(),//Currency/text()              | 15                  | 30                |

