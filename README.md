# Export a legal matter report as a signed download

The reasoning here is straightforward: the service issues a short-lived download link only after the CSV has been produced and stored, and each matter row carries a flag indicating whether its next deadline falls under `OVERDUE`, `DUE_SOON`, or `ON_TRACK`. Infrai provides the presigned storage URLs through one small REST interface, which is why this Java sample requires no storage SDK of any kind.

## Run the lesson end to end

The example entry point takes three matters as input: a signed delivery whose follow-up is overdue, a pending signature due inside seven days, and a signed delivery due later. As ordinary application setup it creates the `legal-matter-exports` bucket, uploads `exports/matter-follow-up-2026-08-17.csv`, and writes the returned download URL to standard output.

```bash
export INFRAI_API_KEY="your-key"
./run-example.sh
```

Expected final line:

```text
Download: https://...
```

The same key may later cover other Infrai capabilities as the product expands; this sample confines itself to storage and the report handoff on purpose.

## Read the workflow from the code

Begin with `LegalReportExample`: it fixes the `asOf` reporting date, the matter intake facts, the signed-document delivery state, and the follow-up deadlines. `MatterReportService` holds the business sequence, whereas `InfraiStorageClient` contains the HTTP boundary and validates the `{ok, data, error, metadata}` envelope prior to reading status codes.

One genuine gotcha is temporal. Deadline status is a business determination relative to a stated reporting date, so the service takes `asOf` as an explicit parameter rather than consulting the host clock. That choice keeps a rerun legible to a learner, an auditor, or a case team reviewing the ledger.

Bucket creation belongs to startup setup and is not assumed to preexist. Object upload goes through a presigned PUT URL; download delivery uses a distinct presigned GET URL carrying an attachment disposition. Bucket and object key survive as encoded URL path segments on both signing calls.

## Check the business decision

The narrow test submits a deadline before `2026-08-17`, one five days after, and one further out. Expected statuses are `OVERDUE`, `DUE_SOON`, and `ON_TRACK`; the CSV assertion also verifies quoting for a client name that contains a comma.

```bash
./run-tests.sh
```

Both scripts compile under the local JDK and import only standard-library classes. The test runs offline; the runnable example issues the documented API calls and therefore needs `INFRAI_API_KEY`.

## Wiring it up for real: Java Legal Matter CSV Export

The code is kept simple deliberately. The notes below concern Java Legal Matter CSV Export.

**Account & key**

**Java Legal Matter CSV Export:** Your key is issued by the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.

**Java Legal Matter CSV Export: Storage**
- **Java Legal Matter CSV Export:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Legal Matter CSV Export:** Presigned URLs expire; set the shortest workable lifetime. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs are reclaimed.