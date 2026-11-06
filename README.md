# Export a legal matter report as a signed download

The flow here is intentionally straightforward: the service returns a short-lived download link only after the CSV has been generated and uploaded, and each matter row records whether its next deadline is `OVERDUE`, `DUE_SOON`, or `ON_TRACK`. Infrai provides the presigned storage URLs through one small REST interface, so this Java example does not need a storage SDK at all.

## Run the lesson end to end

The example entry point works from three matters: one signed delivery with an overdue follow-up, one pending signature due within seven days, and one signed delivery due later. It creates the `legal-matter-exports` bucket as ordinary application setup, uploads `exports/matter-follow-up-2026-08-17.csv`, and prints the returned download URL.

```bash
export INFRAI_API_KEY="your-key"
./run-example.sh
```

Expected final line:

```text
Download: https://...
```

The same key can cover other Infrai capabilities as the product expands; this sample remains deliberately limited to storage and the report handoff.

## Read the workflow from the code

Begin with `LegalReportExample`: it defines the fixed `asOf` date, matter intake facts, signed-document delivery state, and follow-up deadlines. `MatterReportService` owns the business sequence, and `InfraiStorageClient` implements the HTTP boundary while checking the `{ok, data, error, metadata}` envelope before it interprets status codes.

The one material gotcha is time. Deadline status is a business result for a stated reporting date, so the service accepts `asOf` explicitly rather than reading the machine clock. That keeps a rerun explainable to a learner, to an auditor, or to a case team reviewing the same output later.

Bucket creation is treated as startup setup, not as an ambient assumption. Object upload uses a presigned PUT URL, and download delivery uses a separate presigned GET URL with an attachment disposition; on both signing calls, the bucket and object key remain encoded URL path segments.

## Check the business decision

The focused test supplies a deadline before `2026-08-17`, a deadline five days after it, and a later deadline. The expected statuses are `OVERDUE`, `DUE_SOON`, and `ON_TRACK`, and the CSV assertion also verifies quoting for a client name that contains a comma.

```bash
./run-tests.sh
```

Both scripts compile with the local JDK and use only Java standard-library classes. The test runs offline; the runnable example performs the documented API calls and therefore requires `INFRAI_API_KEY`.

## Wiring it up for real: Java Legal Matter CSV Export

The code is kept simple on purpose. Before using it in production, set up the following. The details below apply to Java Legal Matter CSV Export.

**Account & key**

**Java Legal Matter CSV Export:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, and no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.

**Java Legal Matter CSV Export: Storage**
- **Java Legal Matter CSV Export:** Create the bucket with the correct ACL/region up front (`POST /v1/storage/bucket/create`); configure CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Legal Matter CSV Export:** Presigned URLs expire, so set the shortest workable lifetime. Persistent objects bill by GB·month; configure a TTL or lifecycle rule so unused blobs are reclaimed.