# LO1 Evidence — Requirements Document

This document provides a set of requirements for the ILP Drone Delivery Microservice.  

It is **not** a full system specification.  
Its purpose is to present a **diverse, multi-level, testable set of requirements** including:

- System-level functional requirements  
- Integration-level requirements  
- Unit-level requirements  
- Measurable attributes  
- Operational and quality requirements  

Each requirement includes:

- **Level** (unit / integration / system / operational)
- **Description** of the required behaviour
- **Rationale** explaining why the requirement is necessary
- **Test approach** outlining how the requirement would be exercised
- **Appropriateness** justifying why the chosen test approach is suitable
- **Weaknesses** identifying limitations or risks in testing the requirement

---

# 1. Scope (High-Level Overview)

The microservice under test is a Java Spring Boot application used as the basis
for this portfolio. The scope of this LO1 evidence focuses on a selected subset
of its intended behaviour, including:

- Geometry-related primitives for drone navigation (CW1)
- Interaction with the ILP backend for drones, service points, restricted areas,
  and availability (CW2)
- Delivery plan computation involving pathfinding, drone selection, and cost calculation
- Selected operational and quality-related behaviours relevant to testability

This LO1 evidence documents **requirements only**. It does not claim completeness
of system specification, nor does it report implementation details or testing results.

---

# 2. System-Level Functional Requirements

## FR-S1 — Valid or empty plan must be returned  
**Level:** System  
**Description:** The service must return either a complete delivery plan or an empty plan with cost=0 and moves=0.  
**Rationale:** Ensures deterministic behaviour in success/failure cases.

**Test Approach:** Scenario-based system testing using black-box API workflows, including cases with feasible routes and boundary cases where no safe path exists, asserting either a complete plan or a zero-cost, zero-move empty plan.  

**Appropriateness:** Captures full system behaviour across components where the distinction between valid and empty plans emerges only from end-to-end interaction and cannot be validated at unit or integration level without oversimplifying failure causes.

**Weaknesses:** Hard to isolate the root cause of empty plans, as failures may originate from pathfinding, drone filtering, or backend data variability.

---

## FR-S2 — All computed paths must avoid restricted areas  
**Level:** System  
**Description:** For every consecutive pair of coordinates in any returned path, the movement segment must not intersect the interior of any restricted polygon. Boundary contact (edge/vertex) is treated as blocked.  
**Rationale:** A path is executed as continuous movement between returned waypoints; safety must hold for the entire step, not only the endpoints.

**Test Approach:** System-level testing with synthetic restricted polygons, checking generated paths against inside, outside, edge-on, and vertex-on cases, including small epsilon offsets near polygon boundaries.

**Appropriateness:** Realistically simulates operational safety conditions because restricted-area violations arise from accumulated path decisions and floating-point effects that cannot be exhaustively captured by isolated geometric unit tests.

**Weaknesses:** Floating-point precision near polygon edges may cause borderline cases to behave inconsistently, and exhaustive geometric coverage is infeasible.

---

## FR-S3 — Drone selection must respect capacity & capability  
**Level:** System  
**Description:** Drones must satisfy capacity, cooling/heating, and incompatibility constraints.  
**Rationale:** Ensures valid drone assignment.

**Test Approach:** Scenario-based system tests using synthetic drone datasets that isolate single constraints (capacity, cooling, heating) as well as combined constraint cases, comparing results against known expected selections.

**Appropriateness:** Matches decision rules precisely as drone eligibility depends on the interaction of multiple constraints whose combined effect cannot be reliably validated through independent unit checks.

**Weaknesses:** Coverage depends on the variety of available backend or synthetic datasets, and rare constraint combinations may be under-represented.

---

## FR-S4 — Delivery plans must include outbound and return segments  
**Level:** System  
**Description:** Plans must include outbound paths for deliveries and return paths (`deliveryId = null`).  
**Rationale:** Required for cost consistency and final drone state.

**Test Approach:** Black-box plan-structure validation via the API, asserting the presence and ordering of outbound delivery segments and corresponding return segments.

**Appropriateness:** Tests observable behaviour through the API because correctness is defined by plan structure rather than internal pathfinding decisions, making black-box validation more appropriate than structural inspection.

**Weaknesses:** Faults observed in return segments may originate from pathfinding logic rather than planning structure, making diagnosis indirect.

---

# 3. Integration-Level Functional Requirements

## FR-I1 — ILP backend must be queried once per planning request  
**Level:** Integration  
**Description:** `calcDeliveryPath` must request drones, service points, restricted areas, and availability per invocation.  
**Rationale:** Ensures fresh ILP data.

**Test Approach:** Integration testing with a mocked ILP client, verifying per-request call counts for drones, service points, restricted areas, and availability endpoints.

**Appropriateness:** Mocking isolates backend behaviour while allowing precise verification of interaction frequency, which would be infeasible and nondeterministic when relying on the real ILP service.

**Weaknesses:** Mocking removes real network and service-side failure modes, potentially hiding latency or availability issues.

---

## FR-I2 — Geometry utilities must be invoked during pathfinding  
**Level:** Integration  
**Description:** `distanceBetween`, `nextPosition`, and `isInRegion` must be called during full-path search.  
**Rationale:** Ensures CW1 logic feeds into CW2 planning.

**Test Approach:** Integration testing using spy wrappers around geometry utilities to assert invocation of distanceBetween, nextPosition, and isInRegion during path search.

**Appropriateness:** Verifies internal interaction pathways since correct invocation of geometry utilities cannot be inferred solely from output behaviour without risking false confidence due to coincidental correctness.

**Weaknesses:** Spy instrumentation may slightly affect execution timing or behaviour, though this is acceptable for interaction-level validation.

---

## FR-I3 — Query endpoints must correctly filter drones  
**Level:** Integration  
**Description:** `/query`, `/queryAsPath`, `/queryAvailableDrones` must apply rules consistently.  
**Rationale:** Ensures coherent filtering logic.

**Test Approach:** Parameterized integration tests exercising combinations of query attributes and operators (e.g. <, >, =), including empty-result cases, to ensure consistent filtering across endpoints.

**Appropriateness:** Matches multi-component filtering by exercising parameter combinations across integrated components that cannot be meaningfully validated by isolated unit tests of individual filters.

**Weaknesses:** Operator and attribute coverage is limited by the representativeness of available test datasets.

---

# 4. Unit-Level Requirements

## FR-U1 — distanceBetween computes accurate Euclidean distance  
**Level:** Unit  
**Description:** Must compute correct degree-space Euclidean distance.  
**Rationale:** Core primitive for ordering and estimation.

**Test Approach:** Oracle-based unit tests using known coordinate pairs, including symmetry and zero-distance checks.

**Appropriateness:** Deterministic and mathematically verifiable, making unit-level oracle testing sufficient and rendering higher-level integration tests unnecessary for establishing correctness.

**Weaknesses:** Floating-point arithmetic introduces minor rounding errors that must be tolerated within numeric thresholds.

---

## FR-U2 — nextPosition moves exactly one STEP  
**Level:** Unit  
**Description:** Given a valid angle (multiple of 22.5°), output is exactly one STEP away.  
**Rationale:** Ensures grid correctness.

**Test Approach:** Exhaustive unit testing over all 16 valid angles (multiples of 22.5°) and rejection tests for invalid angles, verifying the STEP distance invariant.

**Appropriateness:** Small, self-contained computation with a finite input domain, allowing exhaustive angle coverage at unit level without combinatorial explosion.

**Weaknesses:** These tests do not validate how movement interacts with higher-level pathfinding logic.

---

## FR-U3 — isInRegion must treat boundary points as inside  
**Level:** Unit  
**Description:** Edge and vertex points count as inside.  
**Rationale:** Ensures consistent polygon semantics.

**Test Approach:** Boundary-value unit tests using simple polygons with points exactly on edges and vertices, as well as minimal inside/outside offsets.

**Appropriateness:** Ideal for boundary-value testing as isolating polygon semantics at unit level avoids confounding effects from pathfinding and floating-point accumulation present at higher levels.

**Weaknesses:** Simplified polygon shapes may not fully reflect complex real-world restricted areas.

---

# 5. System-level measurable requirements

## MR-1 — Requests complete within 30 seconds  
**Level:** System  
**Description:** All endpoints must finish within container time limit.  
**Rationale:** Prevents automarker timeouts.

**Test Approach:** System-level performance testing using stress scenarios with large restricted areas and long routes, measuring execution time across repeated runs and reporting median or percentile timings.

**Appropriateness:** Directly targets measurable performance since execution time depends on integrated pathfinding behaviour and cannot be meaningfully estimated through mocked or isolated components.

**Weaknesses:** Execution time is non-deterministic across environments; repeated measurements reduce but do not eliminate variability.

---

## MR-2 — Pathfinding node expansion is capped  
**Level:** System  
**Description:** A* expansion count must not exceed safe limit.  
**Rationale:** Prevents runaway CPU use.

**Test Approach:** Construction of near worst-case maps to trigger high A* expansion, asserting that expansion count remains below a defined cap or that bounded fallback behaviour is activated.

**Appropriateness:** Validates operational safety constraints by empirically bounding worst-case behaviour where formal analysis of A expansion would be impractical within project constraints. 

**Weaknesses:** Hard to replicate exact expansion conditions.

---

## MR-3 — Cost formula must be consistent  
**Level:** System  
**Description:** Cost = initial + final + costPerMove × stepsUsed.  
**Rationale:** Ensures predictable pricing model.

**Test Approach:** Oracle-based tests using known paths and step counts, comparing computed costs against expected values from the pricing formula.

**Appropriateness:** Cost functions are deterministic and measurable, making oracle-based unit and integration tests sufficient without requiring system-level exploration.

**Weaknesses:** Relies on accurate step counting from pathfinding.

---

# 6. Operational / Quality Requirements

## QR-1 — Default ILP endpoint used if ILP_ENDPOINT is missing  
**Level:** Operational  
**Description:** System must fall back to default backend URL.  
**Rationale:** Ensures deployability.

**Test Approach:** Operational testing by unsetting the environment variable in a containerised test setup and verifying fallback to the default backend endpoint.

**Appropriateness:** Matches operational environment conditions by explicitly testing configuration fallbacks that cannot be validated through static code inspection alone.

**Weaknesses:** Container configuration behaviour may differ slightly from production deployment environments.

---

## QR-2 — Invalid geometry input must produce 400, not 500  
**Level:** System  
**Description:** Malformed coordinates/regions must return controlled errors.  
**Rationale:** Ensures robustness.

**Test Approach:** Negative testing with malformed JSON payloads and invalid numeric values, asserting controlled 400-level responses.

**Appropriateness:** Standard negative testing ensures controlled failure modes, which are critical for robustness but easily overlooked by functional-only testing.

**Weaknesses:** Framework-level validation may intercept some errors before controller logic, limiting branch-level control.

---

## QR-3 — JSON responses must be deterministic  
**Level:** System  
**Description:** Identical inputs produce identical outputs.  
**Rationale:** Required for repeatable tests.

**Test Approach:** Repeat-call testing with identical inputs, comparing responses for structural and numeric equality.

**Appropriateness:** Straightforward and clear as determinism is a prerequisite for reliable automated testing and regression detection.

**Weaknesses:** Determinism may be affected by floating-point formatting or collection ordering.

---

# 7. Requirement Priorities

| Requirement | Priority | Reason |
|------------|----------|--------|
| FR-S1, FR-S2 | High | Core correctness and safety |
| FR-S3 | High | Drone assignment feasibility |
| FR-S4 | Medium | Structural completeness of plans |
| FR-U1, FR-U2, FR-U3 | Medium | Foundational geometric primitives |
| FR-I1 | Medium | Backend interaction correctness |
| FR-I2, FR-I3 | Medium | Integration behaviour |
| MR-1, MR-2 | High | Performance and safety constraints |
| MR-3 | Medium | Pricing correctness |
| QR-1 | Medium | Deployment robustness |
| QR-2, QR-3 | Medium | Error handling and determinism |


---

# 8. Dependency Map

### Unit-level primitives

**FR-U1 (distanceBetween)**  
- Does not depend on other requirements.  
- Serves as a foundational geometric primitive used by path construction and cost estimation.

**FR-U2 (nextPosition)**  
- Does not depend on other requirements.  
- Provides discrete movement steps required by grid-based pathfinding.

**FR-U3 (isInRegion)**  
- Does not depend on other requirements.  
- Defines polygon semantics that are later relied upon by restricted-area avoidance.

---

### Integration-level behaviour

**FR-I1 (backend queried once per request)**  
- Does not depend on other functional requirements.  
- Acts as a prerequisite for system-level correctness by ensuring fresh and consistent backend data.

**FR-I2 (geometry utilities invoked during pathfinding)**  
- Depends on FR-U1, FR-U2, and FR-U3.  
- Ensures that unit-level geometry logic is actually exercised during integrated path search.

**FR-I3 (query endpoints filter drones correctly)**  
- Depends on FR-I1 for consistent backend data.  
- Supports system-level drone assignment behaviour across multiple endpoints.

---

### System-level functional behaviour

**FR-S2 (paths avoid restricted areas)**  
- Depends on FR-U3 for polygon semantics and FR-I2 for correct invocation of geometry during pathfinding.  
- Represents emergent safety behaviour that cannot be validated in isolation.

**FR-S4 (outbound and return segments included)**  
- Depends on FR-S2 for valid path construction and FR-I2 for correct path generation logic.  
- Defines structural correctness of delivery plans rather than internal algorithms.

**FR-S3 (drone selection respects constraints)**  
- Depends on FR-I1 for fresh availability data and FR-I3 for consistent filtering logic.  
- Correctness arises from the interaction of multiple integrated constraints.

**FR-S1 (valid or empty plan returned)**  
- Depends on FR-S2, FR-S3, and FR-S4.  
- Represents overall system planning behaviour emerging from pathfinding and assignment outcomes.

---

### System-level measurable requirements

**MR-2 (pathfinding node expansion capped)**  
- Depends on FR-S2 and FR-U2, as expansion behaviour is driven by movement and restricted-area complexity.  
- Acts as a control mechanism for worst-case search behaviour.

**MR-1 (requests complete within 30 seconds)**  
- Depends on MR-2 and is indirectly influenced by FR-S2 and FR-S3.  
- Performance emerges from combined path complexity and drone filtering effectiveness.

**MR-3 (cost formula consistency)**  
- Depends on FR-U1 for distance calculation and FR-S4 for correct path structure.  
- Ensures deterministic pricing once path and step counts are established.

---

### Operational and quality requirements

**QR-1 (default ILP endpoint fallback)**  
- Does not depend on other requirements.  
- Affects FR-I1 by ensuring backend integration remains functional under deployment misconfiguration.

**QR-2 (invalid geometry yields 400, not 500)**  
- Does not define standalone behaviour.  
- Supports FR-S1 by ensuring failure modes are controlled and distinguishable from valid empty plans.

**QR-3 (deterministic JSON responses)**  
- Does not define functional behaviour.  
- Is a cross-cutting prerequisite for reliable testing of system and integration requirements.

---

# 9. Summary

This LO1 Evidence document provides:

- A diverse, multi-level requirement set  
- Explicit level classification  
- Test approaches, appropriateness, and weaknesses  
- Priorities and dependencies  

