**System-level behaviours considered for FR-S2 (restricted-area avoidance)**

| Behaviour class                | Scenario characteristics                                                            | Expected system behaviour                                                                 |
|-------------------------------|--------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------|
| Baseline feasible routing     | Start and goal are outside all restricted areas, and at least one safe path exists   | A non-empty path is generated that satisfies all restricted-area safety constraints       |
| Start or goal invalid         | Start or goal lies inside, or touches, a restricted area                             | No path is generated; the request is rejected or results in an empty plan                 |
| Obstacle-induced detour       | The direct path intersects a restricted area, but a safe detour exists               | A path is generated that avoids all restricted areas while remaining connected            |
| Near-boundary routing         | Paths lie close to restricted-area boundaries without intersecting them              | A path is generated and accepted only if all segments remain strictly outside restrictions|
| Boundary contact as violation | A path segment touches the boundary of a restricted area without strictly entering it| The path is treated as unsafe and rejected according to the safety criterion              |



