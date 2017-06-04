This is application for indexing websites

# API
To add website for indexing, execute POST request to http://localhost/website with body:

```
{
  "url": "https://techcrunch.com",
  "prevButtonXPath": "//li[contains(@class, 'prev')]/a",
  "nextButtonXPath": "//li[contains(@class, 'next')]/a",
  "linkXpath": "//h2[contains(@class, 'post-title')]/a",
  "maxScanDepth": 50,
  "latestPercent": 0.1 
}
```

- **url** - url of website
- **prevButtonXPath** - XPath of button which leads to previous page
- **nextButtonXPath** - XPath of button which leads to next page
- **linkXpath** - XPath of article link
- **maxScanDepth** - how many articles should be indexed initially
- **latestPercent** - what percent of articles should be considered as "latest"

Then execute **/website/scan/all** to index latest **maxScanDepth** articles.

Full API:
http://localhost/swagger-ui.html

- GET /article?url=... - fetch article body
- GET /article/changelog?url=...&v1=...&v2=... - show diff between v1 & v2 revisions of article (first revision is 0)
- GET /article/revision?url=...&v=... - fetch Vth revision of article (first revision is 0)

- GET /website?url=... - fetch website config
- POST /website - set website config
- GET /website/articles - fetch all articles of website
- POST /website/scan/all?url=... - scan **maxScanDepth** latest articles of website
- POST /website/scan/latest?url=... - scan latest articles of website using methodology


# Methodology
It uses Redis for storage, because basically all we need is persistable data structures + K-V storage

Metadata for website, article is stored in K-V model
All links of website are stored in sorted set. Timestamp of article determines order in this set.

Any number of latest articles can be fetched by ZREVRANGE command.
When request for scanning latest articles arrives:

 1. A = Fetch **maxScanDepth * latestPercent** lastly scanned articles
 2. B = Fetch new articles until **maxScanDepth * latestPercent** existing articles are visited
 3. Delete articles = A/B
 4. Remove deleted articles & store B to Redis