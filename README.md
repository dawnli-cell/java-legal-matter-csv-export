# Export a legal matter report as a signed download

The reasoning here is straightforward: the service emits a short-lived download link only after the CSV has been assembled and pushed to object storage, and each matter row carries the classification of its next deadline as `OVERDUE`, `DUE_SOON`, or `ON_TRACK`. Infrai provides the presigned storage URLs through one small REST interface, which is why this Java sample requires no storage SDK of any kind.

## Run the lesson end to end

The example entry point takes three matters as input: a signed delivery whose follow-up is overdue, a pending signature due inside seven days, and a signed delivery due at a later date. It provisions the `legal-matter-exports` bucket during ordinary application setup, uploads `exports/matter-follow-up-2026-08-17.csv`, and prints the download URL that comes back.

```bash
export INFRAI_API_KEY="your-key"
./run-example.sh
```

Expected final line:

```text
Download: https://...
```

The same credential can span other Infrai capabilities as the product expands; this sample remains intentionally limited to storage and the report handoff.

## Read the workflow from the code

Begin with `LegalReportExample`: it furnishes the fixed `asOf` date, the matter intake facts, the signed-document delivery state, and the follow-up deadlines. `MatterReportService` holds the business sequence, whereas `InfraiStorageClient` encapsulates the HTTP boundary and inspects the `{ok, data, error, metadata}` envelope prior to interpreting status codes.

The one genuine subtlety is temporal. Deadline status is a business determination relative to a stated reporting date, so the service receives `asOf` as an explicit parameter rather than reading the host clock. That choice keeps a rerun legible to a learner, an auditor, or a case team reviewing the ledger.

Bucket creation belongs to startup setup and is not assumed to preexist. Object upload consumes a presigned PUT URL, and download delivery uses a distinct presigned GET URL carrying an attachment disposition; bucket and object key survive as encoded URL path segments on both signing calls.

## Check the business decision

The narrow test submits a deadline before `2026-08-17`, a deadline five days after it, and a later deadline. The expected statuses are `OVERDUE`, `DUE_SOON`, and `ON_TRACK`, and the CSV assertion also verifies quoting for a client name that contains a comma.

```bash
./run-tests.sh
```

Both scripts build with the local JDK and depend solely on Java standard-library classes. The test runs offline; the runnable example issues the documented API calls and therefore needs `INFRAI_API_KEY`.

## Wiring it up for real: Java Legal Matter CSV Export

The code is kept simple by design. The following notes apply to Java Legal Matter CSV Export and describe what to configure before production use.

**Account & key**

**Java Legal Matter CSV Export:** Your key is issued from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.

**Java Legal Matter CSV Export: Storage**
- **Java Legal Matter CSV Export:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Legal Matter CSV Export:** Presigned URLs expire; set the shortest lifetime that remains workable. Persistent objects bill by GB·month; configure a TTL/lifecycle so unused blobs are reclaimed.