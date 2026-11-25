#!/usr/bin/env python3
"""Login and fetch every page, reporting status and obvious error markers."""
import http.cookiejar
import re
import sys
import urllib.parse
import urllib.request

BASE = "http://localhost:8080/dompetku"
PAGES = [
    "/app/dashboard",
    "/app/transactions",
    "/app/transactions/new",
    "/app/transactions/edit/1",
    "/app/transactions/import",
    "/app/transactions/trash",
    "/app/categories",
    "/app/categories/new",
    "/app/categories/edit/5",
    "/app/wallets",
    "/app/wallets/new",
    "/app/wallets/edit/2",
    "/app/budgets",
    "/app/budgets/new",
    "/app/goals",
    "/app/goals/new",
    "/app/goals/view/1",
    "/app/goals/deposit/edit/1",
    "/app/debts",
    "/app/debts/new",
    "/app/recurring",
    "/app/recurring/new",
    "/app/reports",
    "/app/profile",
]

jar = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))

login_html = opener.open(BASE + "/login", timeout=20).read().decode("utf-8")
match = re.search(r'name="_csrf" value="([^"]+)"', login_html)
if not match:
    print("FAIL: csrf token not found on login page")
    sys.exit(1)

data = urllib.parse.urlencode({
    "email": "demo@dompetku.test",
    "password": "password",
    "_csrf": match.group(1),
}).encode()
response = opener.open(BASE + "/login", data, timeout=20)
print("login ->", response.geturl())

failures = 0
for page in PAGES:
    try:
        resp = opener.open(BASE + page, timeout=20)
        html = resp.read().decode("utf-8", "replace")
        marker = ""
        if "Terjadi kesalahan" in html or "org.apache.jasper" in html or "javax.servlet.ServletException" in html:
            marker = " ERROR-MARKER"
            failures += 1
        print(f"{resp.status} {page}{marker} ({len(html)} bytes)")
    except Exception as exc:  # noqa: BLE001
        failures += 1
        print(f"FAIL {page}: {exc}")

print("failures:", failures)
sys.exit(1 if failures else 0)
