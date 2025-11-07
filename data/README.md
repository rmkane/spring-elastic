README for Test Data

This directory contains sample documents for testing the Spring Elastic application.

To upload a document, use:

curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@data/sample1.txt"

Or use any of the other sample files in this directory.

Available test files:
- sample1.txt - Basic text document
- sample2.txt - Another text document with different content
- lorem-ipsum.txt - Lorem ipsum placeholder text
- sample.json - JSON formatted document
- technical-doc.md - Markdown formatted technical documentation

