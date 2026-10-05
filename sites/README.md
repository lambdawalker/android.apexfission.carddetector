# Card Detector documentation website

Source ownership and full commands: [maintenance](../docs/maintenance.md).

```bash
npm ci
npm run check
npm run dev -- --host 0.0.0.0
```

Open `/android.apexfission.carddetector/`. Production output is `dist/`. Node 22.12+ is required by the locked Astro toolchain; CI uses Node 24. Generated human pages, media copies, and raw guides are ignored. Edit canonical `docs/` sources, not generated content. No Maven credentials are needed.
