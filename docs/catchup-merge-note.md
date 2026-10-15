# Catch-up merge note

**NOT_READY. Do not integrate this candidate.** Final affected evidence is 35/37 with original growth deviation 264 > the authorized 240 fallback and an unresolved actor-first selected-cargo consumption failure. The other growth control differs by 97. Complete-suite and final-candidate fast acceptance are absent because the owner's explicit stop condition was reached. No master-red exception is claimed.

Both authorized changes and the negative acceptance observers are committed on `catchup`; consult [the report](catchup-report.md) and external catchup4/T01 `raw/t01-acceptance.json` for the exact final commit, source hashes, native exits and fresh XML. Merged/tested master is `14ef11f68a4f55c87ad2b8725ebc748f6f5d8573`, unchanged on the final recheck. Earlier targeted/fast positives are superseded for readiness by the final negative.

Owner-session integration procedure, only after a newly authorized recovery and a READY packet:

1. Verify the candidate is exactly the packet's tested commit and the tested master is its ancestor (`git merge-base --is-ancestor <tested-master> catchup`). Check current `master` against the packet's merged/tested SHA.
2. Review every recorded exception and its own matching native evidence from that exact master SHA. This packet has no such exceptions and does not authorize integration.
3. Require complete, unfiltered, fresh coverage from `./gradlew.bat check assemble -PprimeAntsGameTestConcurrency=2 --console=plain --rerun-tasks`, plus all requested outcome gates. Preserve a nonzero native exit when individually proved master-red exceptions exist.
4. If master has advanced, especially incompatibly, reconcile it into catchup and refresh affected/fast/complete acceptance first. Never treat the old tested relationship as current acceptance.
5. Once READY is established, the owner can switch to master and integrate, for example `git merge --no-ff catchup`.

The executor did not merge into, rebase or push master. Further source design after the 264-step stop requires a new mandate; the frozen 92-tick interval must not be fitted to the growth error.
