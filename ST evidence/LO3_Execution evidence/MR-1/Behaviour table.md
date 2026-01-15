**System-level performance behaviours considered for MR-1**

| Behaviour class                    | Workload characteristics                                              | Expected system behaviour                                                                 |
|-----------------------------------|-----------------------------------------------------------------------|------------------------------------------------------------------------------------------|
| Non-trivial execution behaviour   | Representative or stress workloads with early-exit paths possible     | The system performs full planning work rather than returning trivial fast results         |
| Reproducible timing behaviour     | Repeated executions of the same workload configuration                 | Observed execution times are stable across runs within expected variability               |
| Performance threshold violation   | Workload causes prolonged planning or excessive search expansion        | Request execution exceeds 30 seconds and MR-1 is violated                                 |

