# README for Test Data

This directory contains sample documents for testing the Spring Elastic application.

To upload a document, run **`curl` from the project repository root** (so paths like `data/...` resolve), or use an **absolute** path to the file so it works from any directory.

```sh
# From repo root
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@data/sample1.txt"

curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@data/sample.json"
```

From elsewhere, point `@` at the real path, for example:

```sh
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@/full/path/to/spring-elastic/data/sample.json"
```

If curl reports `Failed to open/read local data from file`, the path after `@` is wrong for your current directory or the filename is mistyped (there is **`sample.json`**, not `sample1.json`).

Available test files:

- `sample1.txt` - Basic text document
- `sample2.txt` - Another text document with different content
- `lorem-ipsum.txt` - Lorem ipsum placeholder text
- `sample.json` - JSON formatted document
- `technical-doc.md` - Markdown formatted technical documentation
