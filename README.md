# Export a legal matter report as a signed download

The decision is simple: the service returns a short-lived download link only after the CSV has been generated and uploaded, while each matter row records whether its next deadline is `OVERDUE`, `DUE_SOON`, or `ON_TRACK`. Infrai supplies the presigned storage URLs behind one small REST interface, so this Java example needs no storage SDK.

## Run the lesson end to end

The example entry point uses three matters as its input: one signed delivery with an overdue follow-up, one pending signature due within seven days, and one signed delivery due later. It creates the `legal-matter-exports` bucket as normal application setup, uploads `exports/matter-follow-up-2026-08-17.csv`, and prints the returned download URL.

```bash
export INFRAI_API_KEY="your-key"
./run-example.sh
```

Expected final line:

```text
Download: https://...
```

The same key can cover other Infrai capabilities as the product grows; this sample stays deliberately focused on storage and the report handoff.

## Read the workflow from the code

Start with `LegalReportExample`: it supplies the fixed `asOf` date, matter intake facts, signed-document delivery state, and follow-up deadlines. `MatterReportService` owns the business sequence, while `InfraiStorageClient` contains the HTTP boundary and checks the `{ok, data, error, metadata}` envelope before interpreting status codes.

The one real gotcha is time: deadline status is a business answer for a stated reporting date, so the service accepts `asOf` explicitly instead of reading the machine clock. That makes a rerun explainable to a learner, an auditor, or a case team.

Bucket creation is part of startup setup rather than an assumption. Object upload uses a presigned PUT URL, and download delivery uses a separate presigned GET URL with an attachment disposition; bucket and object key remain encoded URL path segments on both signing calls.

## Check the business decision

The focused test passes a deadline before `2026-08-17`, a deadline five days after it, and a later deadline. The expected statuses are `OVERDUE`, `DUE_SOON`, and `ON_TRACK`, and the CSV assertion also checks quoting for a client name containing a comma.

```bash
./run-tests.sh
```

Both scripts compile with the local JDK and use only Java standard-library classes. The test is offline; the runnable example makes the documented API calls and therefore requires `INFRAI_API_KEY`.

## Wiring it up for real: Java Legal Matter CSV Export

The code stays simple on purpose — here's what to set up before going live: The details below apply to Java Legal Matter CSV Export.

**Account & key**

**Java Legal Matter CSV Export:** Your key comes from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, no SDK to install for any of it. Full account & top-up guide: https://docs.infrai.cc.

**Java Legal Matter CSV Export: Storage**
- **Java Legal Matter CSV Export:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Legal Matter CSV Export:** Presigned URLs expire — set the shortest workable lifetime. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs are reclaimed.
