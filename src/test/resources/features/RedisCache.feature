
Feature: Redis Cache Server

  @ready
  Scenario Outline:
    Given an API request to retrieve a "<key>" that "<cache_condition>" exists in the cache with value "<value>"
    When GET is called with a "<endpoint_condition>" endpoint
    Then it should return a <status_code>
    Then it should return a response "<response>"
    Then it "<should_condition>" exist in the Redis cache

    Examples:
    | key       | value        | cache_condition | endpoint_condition   | status_code | response | should_condition |
    | A         | valueA       | does            | valid                | 200         | valueA   | should           |
    | B         | valueB       | does_not        | valid                | 200         | None     | should_not       |
    | A         | valueA       | does            | valid                | 200         | valueA   | should           |