**System-level behaviours considered for FR-S3 (capability-based drone selection)**

| Behaviour class               | Scenario characteristics                                                     | Expected system behaviour                                                                 |
|------------------------------|--------------------------------------------------------------------------------|------------------------------------------------------------------------------------------|
| Single eligible candidate    | Exactly one drone satisfies all required capability constraints                | The eligible drone is selected                                                           |
| Multiple eligible candidates | More than one drone satisfies all required capability constraints              | Any drone from the eligible set may be selected; no tie-breaking policy is assumed        |
| Combined constraint exclusion| Drones violate different subsets of the required capability constraints        | All drones violating at least one constraint are excluded from selection                  |
| No eligible candidate        | No drone satisfies all required capability constraints                          | No drone is selected and the request results in an empty or failed response               |



