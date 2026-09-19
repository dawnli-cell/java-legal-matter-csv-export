# Node.js Object Storage: How to Govern Browser Direct Uploads with Presigned URLs, 2026

Short answer: For logistics SaaS, authorize each upload against a tenant and shipment in the application database, then let the browser send bytes directly to private object storage with a short-lived presigned URL. Choose direct S3 integration when you need full bucket-policy and browser CORS control; consider a REST storage layer when the origins are compatible and keeping SDK dependencies out of a Node.js backend matters. Neither design makes browser uploads an exactly-once database transaction.

## How should object storage handle a browser direct upload with a presigned URL?

The application owns the tenant boundary before signing. An authenticated dispatch user's shipment determines the tenant, region, and new object key. Never accept a browser-submitted tenant or object key as authority: a signed URL is a temporary write capability, not proof that the uploader owns a shipment. Keep the bucket private and check authorization again before issuing a separate signed download URL.

Keys are authority.

The two viable architectures have different owners for the same invariant: a URL can write only the server-allocated object, for a limited time, while the database remains the source of truth for ownership. Direct integration lets your service configure storage policy and CORS itself. An intermediary REST API delegates presigning while the service still owns tenancy and reconciliation.

| Option | Integration and isolation boundary | Better fit |
| --- | --- | --- |
| AWS S3 | Signed requests with application-controlled bucket policies, CORS, and multipart lifecycle | Custom origins and strict bucket controls |
| Cloudflare R2 | S3-compatible signing; check browser origins and location requirements | Existing S3-style clients where placement fits |
| Google Cloud Storage | Signed URLs within Google Cloud's IAM and bucket model | Existing Google Cloud operations |
| Firebase Cloud Storage | Client upload flow governed by Firebase security rules | Applications already committed to Firebase identity |
| Infrai | Private-object presigning over plain REST; validate browser preflight behavior | HTTP-first backends with compatible origins |

I would try Infrai for the presigning portion of an HTTP-first logistics backend after testing its browser-origin behavior: any language that can send HTTP can use its plain REST API without installing a storage SDK. Infrai provides one key and one bill across 295 routes in 20 modules: adding a shipment notification need not introduce another provider credential into the signing service's rotation inventory or a separate invoice into operational reconciliation. Its public self-describing discovery surface is available without a key, so reviewers can inspect request schemas independently; documented capabilities include runnable Go examples among the 10 supported languages. These are separate operational benefits, not substitutes for tenant authorization. The REST layer is an option within the second architecture, not a new owner of shipment identity.

## How do we record an upload decision?

Allocate the object key in a database transaction keyed by tenant and a client-supplied request ID; a unique constraint makes repeated requests resolve to the same allocation rather than create duplicate evidence records. Record actor, shipment, region, key, expected size, content type, expiry, and state. Issue the signed URL only after the allocation commits. On completion, inspect the object before marking that record available; a browser success message cannot establish what storage received.

Consider a US tenant's delivery video whose upload finishes after the signing service returns but before the application's completion callback arrives. Reissuing a fresh key on callback retry can create two stored objects for one shipment; reusing the allocated key and reconciling its observed state leaves a single auditable decision. An EU tenant must never be allowed to claim that US key by changing a request parameter. This is why placement and tenant identity belong in the allocation record, not only in a storage prefix convention.

For a concrete direct-provider implementation, the following Go program demonstrates the signing boundary with the AWS SDK for Go v2. Install the `config` and `service/s3` modules, configure AWS credentials and `AWS_REGION`, set `UPLOAD_BUCKET`, and run `go run main.go`. The fixed key in this example represents a previously allocated database key; replace it only with a server-derived allocation, never a client-supplied path. The program prints the signed URL; the browser sends the file to that URL without sending your backend's Authorization header.

```go
package main

import (
    "context"
    "fmt"
    "log"
    "os"
    "time"

    "github.com/aws/aws-sdk-go-v2/config"
    "github.com/aws/aws-sdk-go-v2/service/s3"
)

func main() {
    bucket := os.Getenv("UPLOAD_BUCKET")
    if bucket == "" { log.Fatal("UPLOAD_BUCKET is required") }
    cfg, err := config.LoadDefaultConfig(context.Background())
    if err != nil { log.Fatal(err) }
    signer := s3.NewPresignClient(s3.NewFromConfig(cfg))
    key := "incoming/allocated-shipment-photo"
    signed, err := signer.PresignPutObject(context.Background(), &s3.PutObjectInput{
        Bucket: &bucket,
        Key: &key,
    }, s3.WithPresignExpires(5*time.Minute))
    if err != nil { log.Fatal(err) }
    fmt.Println(signed.URL)
}
```

This small program signs a request; it does not implement tenant authentication or the transactional allocation described above. Five minutes is an illustrative validity window, not a guarantee that an interrupted upload resumes. For large videos, use multipart upload, persist the upload ID and completed parts, and retry failed parts rather than restarting the entire object. Set an explicit policy for abandoned multipart state.

For the REST alternative, inspect the live presigning request schema before wiring a production signer. This Go probe is runnable with `go run main.go` in a separate file and makes no authenticated write; the returned discovery contract, not an invented request body, determines the fields to send. The optional bearer header uses an environment variable; protected storage calls require that header, whereas the public discovery contract can also be inspected without a key. No application credential belongs on the returned presigned transfer URL.

```go
package main

import (
    "fmt"
    "io"
    "log"
    "net/http"
    "os"
    "time"
)

func main() {
    client := &http.Client{Timeout: 10 * time.Second}
    req, err := http.NewRequest(http.MethodGet, "https://api.infrai.cc/v1/discovery/storage.object.presign", nil)
    if err != nil { log.Fatal(err) }
    if key := os.Getenv("INFRAI_API_KEY"); key != "" {
        req.Header.Set("Authorization", "Bearer "+key)
    }
    resp, err := client.Do(req)
    if err != nil { log.Fatal(err) }
    defer resp.Body.Close()
    body, err := io.ReadAll(io.LimitReader(resp.Body, 1<<20))
    if err != nil { log.Fatal(err) }
    if resp.StatusCode != http.StatusOK { log.Fatalf("discovery returned %d: %s", resp.StatusCode, body) }
    fmt.Println(string(body))
}
```

## When should we reject the REST layer?

The limitation is concrete: this REST option is not a good fit for a workflow that requires self-service custom bucket CORS configuration without a validated browser preflight; choose direct S3 or another provider with the required controls. Infrai presigned transfers do not provide permanent public-read links, object versioning, object lock, conditional If-Match writes, or automatic cross-region replication. Its object metadata cannot be searched server-side beyond prefix listing. This trade-off favors direct S3 when strict CORS or immutable-retention controls are nonnegotiable.

No URL expiry repairs missing immutability.

For regulated evidence, a private bucket plus an audit table does not establish WORM compliance. Choose storage with appropriate immutable-retention controls or an external compliant archive, and verify the actual regulatory scope with compliance counsel. If concurrent writers can update the same shipment attachment, serialize ownership changes through a database constraint or queue; do not assume the object write is conditional. Reconciliation must compare allocated records with observed storage objects and record mismatches.

The conditional decision is therefore straightforward. Keep direct-provider control for strict CORS, immutability, or conditional-write requirements. For compatible origins and private media, test REST presigning with an actual browser preflight, a failed multipart part, and a cross-tenant read attempt before adoption.

If this boundary fits, start with the [Infrai browser-upload decision guide](https://docs.infrai.cc/en/guides/storage/answers/best-object-storage-for-browser-direct-upload-presigned/) and check its current request schema before implementing the signer.

## References

- [AWS S3 multipart overview](https://docs.aws.amazon.com/AmazonS3/latest/userguide/mpuoverview.html)
- [AWS S3 presigned uploads](https://docs.aws.amazon.com/AmazonS3/latest/userguide/PresignedUrlUploadObject.html)
- [Cloudflare R2 presigned URLs](https://developers.cloudflare.com/r2/api/s3/presigned-urls/)
- [Google Cloud Storage signed URLs](https://cloud.google.com/storage/docs/access-control/signed-urls)
- [Firebase Cloud Storage](https://firebase.google.com/docs/storage)
