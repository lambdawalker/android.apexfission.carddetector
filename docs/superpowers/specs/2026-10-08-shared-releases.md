# Shared module release identity

Approved behavior: carddetector and tfmodel keep independent semantic versions, shared by all publication destinations. Reusing unchanged release inputs republishes the original tagged source to another destination without allocating a version. Changed inputs on a descendant source allocate the next patch. Historical refs remain immutable. Pending publication is not availability.

Canonical refs are module/vX.Y.Z; destination journals stay separate. Same-number historical releases with different source identities fail closed. Release inputs cover selected source/assets/packaged docs and shared build dependencies, excluding generated publication metadata and unrelated sibling sources. Reused source must already contain the new build protocol; historical unsupported sources require a new release. Build/config changes count as release input changes.

JitPack receives a module-scoped tag and builds only that module. Its consumer version is the tag with slash replaced by tilde. Public verification checks build provenance and actual artifacts, with no expectation of Central signatures or identical build bytes. Model releases retain an external stable core dependency. Finalization updates only its destination record while preserving concurrent sibling/destination records.

IMPORT.md selects each module's highest confirmed semantic version, showing all matching destinations only when source identity agrees. It includes actual coordinates and dependency repositories. No historical documentation system is introduced.
