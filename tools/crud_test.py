#!/usr/bin/env python3
"""End-to-end CRUD, security, validation, and authorization tests."""
import datetime
import http.cookiejar
import re
import subprocess
import sys
import urllib.error
import urllib.parse
import urllib.request

BASE = "http://localhost:8080/dompetku"
MYSQL = r"C:\xampp\mysql\bin\mysql.exe"
TODAY = datetime.date.today().isoformat()
MONTH = datetime.date.today().strftime("%Y-%m")
OVERDUE = (datetime.date.today() - datetime.timedelta(days=90)).isoformat()
NAME = "QA Test"

jar = http.cookiejar.CookieJar()
opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar))


def sql(query):
    out = subprocess.check_output([MYSQL, "-u", "root", "dompetku", "-N", "-B", "-e", query], text=True)
    return out.strip()


def csrf(html):
    match = re.search(r'name="_csrf" value="([^"]+)"', html)
    if not match:
        raise RuntimeError("CSRF token missing")
    return match.group(1)


def get(path):
    response = opener.open(BASE + path, timeout=20)
    return response.read().decode("utf-8", "replace")


def post(path, data, expect=None):
    payload = dict(data)
    payload["_csrf"] = TOKEN
    response = opener.open(BASE + path, urllib.parse.urlencode(payload).encode(), timeout=20)
    html = response.read().decode("utf-8", "replace")
    if "Terjadi kesalahan" in html or "org.apache.jasper" in html:
        raise AssertionError(f"server error on POST {path}")
    if expect and expect not in html:
        raise AssertionError(f"missing flash '{expect}' on POST {path}")
    return html


def expect_http_error(path, data, code, method="POST"):
    if method == "POST":
        payload = dict(data or {})
        payload["_csrf"] = TOKEN
        request = urllib.request.Request(
            BASE + path, data=urllib.parse.urlencode(payload).encode(), method="POST")
    else:
        request = BASE + path
    try:
        opener.open(request, timeout=20)
    except urllib.error.HTTPError as exc:
        check(exc.code == code, f"{method} {path} -> HTTP {code}")
        return
    raise AssertionError(f"expected HTTP {code} for {method} {path}")


def expect_csrf_block(path, data):
    try:
        opener.open(BASE + path, urllib.parse.urlencode(data).encode(), timeout=20)
    except urllib.error.HTTPError as exc:
        check(exc.code == 403, "POST without CSRF token -> HTTP 403")
        return
    raise AssertionError("CSRF protection failed: POST without token was accepted")


def check(condition, message):
    if not condition:
        raise AssertionError(message)
    print("  OK:", message)


cleanup_sql = """
DELETE FROM users WHERE email IN ('qa@dompetku.test', 'qa2@dompetku.test');
DELETE FROM recurring_transactions WHERE name LIKE 'QA Test%';
DELETE FROM debts WHERE counterpart LIKE 'QA Test%';
DELETE FROM goals WHERE name LIKE 'QA Test%';
DELETE FROM budgets WHERE category_id IN (SELECT id FROM categories WHERE name LIKE 'QA Test%');
DELETE FROM transactions WHERE note LIKE 'QA Test%';
DELETE FROM transactions_archive WHERE note LIKE 'QA Test%';
DELETE FROM audit_logs WHERE details LIKE '%QA Test%';
DELETE FROM wallets WHERE name LIKE 'QA Test%';
DELETE FROM categories WHERE name LIKE 'QA Test%';
"""

try:
    login_html = get("/login")
    TOKEN = csrf(login_html)
    login_result = opener.open(BASE + "/login", urllib.parse.urlencode({
        "email": "demo@dompetku.test", "password": "password", "_csrf": TOKEN
    }).encode(), timeout=20)
    dashboard = login_result.read().decode("utf-8", "replace")
    check("Dashboard" in dashboard, "login and dashboard session")
    TOKEN = csrf(dashboard)

    print("\n[1] Category CRUD")
    post("/app/categories/create", {
        "name": NAME + " Category", "type": "expense", "color": "#0ea5e9", "icon": "tag"
    }, "berhasil")
    cat_id = int(sql("SELECT id FROM categories WHERE name = 'QA Test Category'"))
    check(cat_id > 0, "category created (id %d)" % cat_id)
    post("/app/categories/update", {
        "id": cat_id, "name": NAME + " Category 2", "type": "expense", "color": "#10b981", "icon": "coffee"
    }, "berhasil")
    check(sql("SELECT name FROM categories WHERE id = %d" % cat_id) == "QA Test Category 2", "category updated")
    duplicate = post("/app/categories/create", {
        "name": NAME + " Category 2", "type": "expense", "color": "#10b981", "icon": "tag"
    })
    check("sudah ada" in duplicate, "duplicate category rejected")

    print("\n[2] Wallet CRUD")
    post("/app/wallets/create", {
        "name": NAME + " Wallet", "type": "cash", "initialBalance": "1000000", "color": "#f59e0b"
    }, "berhasil")
    wallet_id = int(sql("SELECT id FROM wallets WHERE name = 'QA Test Wallet'"))
    check(wallet_id > 0, "wallet created (id %d)" % wallet_id)
    post("/app/wallets/update", {
        "id": wallet_id, "name": NAME + " Wallet 2", "type": "ewallet",
        "initialBalance": "1500000", "color": "#8b5cf6"
    }, "berhasil")
    check(sql("SELECT initial_balance FROM wallets WHERE id = %d" % wallet_id) == "1500000.00",
          "wallet updated")

    print("\n[3] Transaction CRUD + validation")
    post("/app/transactions/create", {
        "type": "expense", "amount": "50000", "walletId": wallet_id,
        "categoryId": cat_id, "date": TODAY, "note": "QA Test Tx"
    }, "berhasil")
    tx_id = int(sql("SELECT id FROM transactions WHERE note = 'QA Test Tx' ORDER BY id DESC LIMIT 1"))
    check(sql("SELECT amount FROM transactions WHERE id = %d" % tx_id) == "50000.00",
          "transaction created (id %d)" % tx_id)
    post("/app/transactions/update", {
        "id": tx_id, "type": "expense", "amount": "75000", "walletId": wallet_id,
        "categoryId": cat_id, "date": TODAY, "note": "QA Test Tx"
    }, "berhasil")
    check(sql("SELECT amount FROM transactions WHERE id = %d" % tx_id) == "75000.00",
          "transaction updated")
    post("/app/transactions/delete", {"id": tx_id}, "berhasil")
    check(sql("SELECT COUNT(*) FROM transactions WHERE id = %d" % tx_id) == "0", "transaction deleted")

    negative = post("/app/transactions/create", {
        "type": "expense", "amount": "-5000", "walletId": wallet_id,
        "categoryId": cat_id, "date": TODAY, "note": "QA Test Invalid"
    })
    check("Nominal harus lebih besar dari 0" in negative, "negative amount rejected")
    invalid_date = post("/app/transactions/create", {
        "type": "expense", "amount": "10000", "walletId": wallet_id,
        "categoryId": cat_id, "date": "", "note": "QA Test Invalid"
    })
    check("Tanggal transaksi wajib diisi" in invalid_date, "empty date rejected")
    mismatch = post("/app/transactions/create", {
        "type": "expense", "amount": "10000", "walletId": wallet_id,
        "categoryId": 1, "date": TODAY, "note": "QA Test Invalid"
    })
    check("Kategori tidak sesuai" in mismatch, "category/type mismatch rejected")
    check(sql("SELECT COUNT(*) FROM transactions WHERE note = 'QA Test Invalid'") == "0",
          "invalid transactions were not stored")

    print("\n[4] Budget CRUD + unique constraint")
    post("/app/budgets/create", {
        "categoryId": cat_id, "month": MONTH, "amount": "500000"
    }, "berhasil")
    budget_id = int(sql("SELECT id FROM budgets WHERE category_id = %d" % cat_id))
    check(budget_id > 0, "budget created (id %d)" % budget_id)
    duplicate_budget = post("/app/budgets/create", {
        "categoryId": cat_id, "month": MONTH, "amount": "700000"
    })
    check("sudah ada" in duplicate_budget, "duplicate budget rejected")
    post("/app/budgets/update", {
        "id": budget_id, "categoryId": cat_id, "month": MONTH, "amount": "600000"
    }, "berhasil")
    check(sql("SELECT amount FROM budgets WHERE id = %d" % budget_id) == "600000.00", "budget updated")
    post("/app/budgets/delete", {"id": budget_id, "returnMonth": MONTH}, "berhasil")
    check(sql("SELECT COUNT(*) FROM budgets WHERE id = %d" % budget_id) == "0", "budget deleted")

    print("\n[5] Goal + deposit CRUD (including edit)")
    post("/app/goals/create", {
        "name": NAME + " Goal", "targetAmount": "1000000",
        "deadline": "2027-12-31", "color": "#10b981"
    }, "berhasil")
    goal_id = int(sql("SELECT id FROM goals WHERE name = 'QA Test Goal'"))
    check(goal_id > 0, "goal created (id %d)" % goal_id)
    post("/app/goals/deposit", {
        "goalId": goal_id, "amount": "250000", "date": TODAY, "note": "QA Test deposit"
    }, "berhasil")
    check(sql("SELECT saved_amount FROM goals WHERE id = %d" % goal_id) == "250000.00",
          "deposit updates saved amount")
    deposit_id = int(sql("SELECT id FROM goal_deposits WHERE goal_id = %d" % goal_id))
    post("/app/goals/deposit/update", {
        "id": deposit_id, "goalId": goal_id, "amount": "300000",
        "date": TODAY, "note": "QA Test deposit updated"
    }, "berhasil")
    check(sql("SELECT saved_amount FROM goals WHERE id = %d" % goal_id) == "300000.00",
          "deposit edit adjusts saved amount")
    check(sql("SELECT amount FROM goal_deposits WHERE id = %d" % deposit_id) == "300000.00",
          "deposit amount updated")
    post("/app/goals/deposit/delete", {"id": deposit_id, "goalId": goal_id}, "berhasil")
    check(sql("SELECT saved_amount FROM goals WHERE id = %d" % goal_id) == "0.00",
          "deposit deletion restores saved amount")
    post("/app/goals/update", {
        "id": goal_id, "name": NAME + " Goal 2", "targetAmount": "1200000",
        "deadline": "2028-01-31", "color": "#8b5cf6"
    }, "berhasil")
    check(sql("SELECT target_amount FROM goals WHERE id = %d" % goal_id) == "1200000.00", "goal updated")
    post("/app/goals/delete", {"id": goal_id}, "berhasil")
    check(sql("SELECT COUNT(*) FROM goals WHERE id = %d" % goal_id) == "0", "goal deleted")

    print("\n[6] Debt CRUD + payment")
    post("/app/debts/create", {
        "type": "payable", "counterpart": NAME + " Friend", "amount": "100000",
        "paidAmount": "0", "dueDate": "2027-01-15", "note": "QA Test debt"
    }, "berhasil")
    debt_id = int(sql("SELECT id FROM debts WHERE counterpart = 'QA Test Friend'"))
    check(debt_id > 0, "debt created (id %d)" % debt_id)
    post("/app/debts/pay", {"id": debt_id, "amount": "40000"}, "berhasil")
    check(sql("SELECT paid_amount FROM debts WHERE id = %d" % debt_id) == "40000.00", "debt payment recorded")
    post("/app/debts/update", {
        "id": debt_id, "type": "receivable", "counterpart": NAME + " Friend 2",
        "amount": "120000", "paidAmount": "40000", "dueDate": "2027-02-01", "note": "QA Test debt"
    }, "berhasil")
    check(sql("SELECT type FROM debts WHERE id = %d" % debt_id) == "receivable", "debt updated")
    post("/app/debts/delete", {"id": debt_id}, "berhasil")
    check(sql("SELECT COUNT(*) FROM debts WHERE id = %d" % debt_id) == "0", "debt deleted")

    print("\n[7] Recurring CRUD + overdue catch-up")
    post("/app/recurring/create", {
        "name": NAME + " Recurring", "type": "expense", "walletId": wallet_id,
        "categoryId": cat_id, "amount": "25000", "frequency": "monthly",
        "nextDate": TODAY, "active": "on"
    }, "berhasil")
    rec_id = int(sql("SELECT id FROM recurring_transactions WHERE name = 'QA Test Recurring'"))
    check(rec_id > 0, "recurring created (id %d)" % rec_id)
    post("/app/recurring/toggle", {"id": rec_id, "active": "0"}, "dijeda")
    check(sql("SELECT active FROM recurring_transactions WHERE id = %d" % rec_id) == "0", "recurring paused")
    post("/app/recurring/toggle", {"id": rec_id, "active": "1"}, "diaktifkan")
    post("/app/recurring/run", {"id": rec_id}, "berhasil")
    check(sql("SELECT COUNT(*) FROM transactions WHERE note = 'QA Test Recurring (otomatis)'") == "1",
          "recurring run creates one transaction")
    generated_tx = int(sql("SELECT id FROM transactions WHERE note = 'QA Test Recurring (otomatis)' LIMIT 1"))
    post("/app/transactions/delete", {"id": generated_tx}, "berhasil")
    post("/app/recurring/delete", {"id": rec_id}, "berhasil")

    post("/app/recurring/create", {
        "name": NAME + " Overdue", "type": "expense", "walletId": wallet_id,
        "categoryId": cat_id, "amount": "10000", "frequency": "monthly",
        "nextDate": OVERDUE, "active": "on"
    }, "berhasil")
    overdue_id = int(sql("SELECT id FROM recurring_transactions WHERE name = 'QA Test Overdue'"))
    post("/app/recurring/run", {"id": overdue_id}, "tertunggak")
    overdue_count = int(sql("SELECT COUNT(*) FROM transactions WHERE note = 'QA Test Overdue (otomatis)'"))
    check(overdue_count >= 2, "overdue recurring creates all missed periods (%d)" % overdue_count)
    next_date = sql("SELECT next_date FROM recurring_transactions WHERE id = %d" % overdue_id)
    check(next_date > TODAY, "overdue recurring advances next date to the future (%s)" % next_date)
    sql("DELETE FROM transactions WHERE note = 'QA Test Overdue (otomatis)'")
    post("/app/recurring/delete", {"id": overdue_id}, "berhasil")

    print("\n[8] XSS output encoding")
    xss_name = 'QA Test XSS "><img src=x onerror=alert(1)>'
    post("/app/categories/create", {
        "name": xss_name, "type": "expense", "color": "#ef4444", "icon": "tag"
    }, "berhasil")
    categories_html = get("/app/categories")
    check("&quot;&gt;&lt;img src=x onerror=alert(1)&gt;" in categories_html,
          "category payload is HTML-escaped")
    check(xss_name not in categories_html, "raw category payload is not rendered")
    xss_cat_id = int(sql("SELECT id FROM categories WHERE name = %s" % "'" + xss_name.replace("'", "''") + "'"))

    xss_note = "QA Test XSS <script>alert('xss')</script>"
    post("/app/transactions/create", {
        "type": "expense", "amount": "1000", "walletId": wallet_id,
        "categoryId": xss_cat_id, "date": TODAY, "note": xss_note
    }, "berhasil")
    ledger_html = get("/app/transactions")
    check("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;" in ledger_html,
          "transaction note payload is HTML-escaped")
    check(xss_note not in ledger_html, "raw transaction note is not rendered")
    xss_tx_id = int(sql("SELECT id FROM transactions WHERE note = 'QA Test XSS <script>alert(''xss'')</script>'"))
    post("/app/transactions/delete", {"id": xss_tx_id}, "berhasil")
    post("/app/categories/delete", {"id": xss_cat_id}, "Kategori dihapus")

    print("\n[9] Delete guards")
    guard = post("/app/wallets/delete", {"id": 2}, "tidak bisa dihapus")
    check("tidak bisa dihapus" in guard, "wallet with transactions cannot be deleted")
    check(sql("SELECT COUNT(*) FROM wallets WHERE id = 2") == "1", "protected wallet still exists")

    print("\n[10] Cleanup test wallet/category")
    sql("DELETE FROM transactions_archive WHERE wallet_id = %d" % wallet_id)
    post("/app/wallets/delete", {"id": wallet_id}, "berhasil")
    post("/app/categories/delete", {"id": cat_id}, "Kategori dihapus")
    check(sql("SELECT COUNT(*) FROM wallets WHERE id = %d" % wallet_id) == "0", "test wallet deleted")
    check(sql("SELECT COUNT(*) FROM categories WHERE id = %d" % cat_id) == "0", "test category deleted")

    print("\n[11] Registration, profile, authorization, CSRF")
    opener.open(BASE + "/logout", urllib.parse.urlencode({"_csrf": TOKEN}).encode(), timeout=20)
    register_html = get("/register")
    register_token = csrf(register_html)
    missing_consent = opener.open(BASE + "/register", urllib.parse.urlencode({
        "name": "QA User", "email": "qa@dompetku.test", "password": "password123!",
        "confirmPassword": "password123!", "_csrf": register_token
    }).encode(), timeout=20).read().decode("utf-8", "replace")
    check("menyetujui ketentuan" in missing_consent, "registration requires consent")
    check(sql("SELECT COUNT(*) FROM users WHERE email='qa@dompetku.test'") == "0",
          "registration without consent does not create a user")
    registered = opener.open(BASE + "/register", urllib.parse.urlencode({
        "name": "QA User", "email": "qa@dompetku.test", "password": "password123!",
        "confirmPassword": "password123!", "agree": "1", "_csrf": register_token
    }).encode(), timeout=20).read().decode("utf-8", "replace")
    check("Selamat datang di Dompetku" in registered, "registration succeeds")
    TOKEN = csrf(registered)
    check(sql("SELECT COUNT(*) FROM categories WHERE user_id = (SELECT id FROM users WHERE email='qa@dompetku.test')") == "13",
          "new user gets 13 default categories")
    check(sql("SELECT COUNT(*) FROM wallets WHERE user_id = (SELECT id FROM users WHERE email='qa@dompetku.test')") == "3",
          "new user gets 3 default wallets")

    post("/app/profile/update", {"name": "QA User Updated", "email": "qa2@dompetku.test"}, "berhasil")
    check(sql("SELECT email FROM users WHERE email = 'qa2@dompetku.test'") == "qa2@dompetku.test",
          "profile email updated")
    old_hash = sql("SELECT password_hash FROM users WHERE email = 'qa2@dompetku.test'")
    post("/app/profile/password", {
        "currentPassword": "password123!", "newPassword": "newpass123!", "confirmPassword": "newpass123!"
    }, "berhasil")
    new_hash = sql("SELECT password_hash FROM users WHERE email = 'qa2@dompetku.test'")
    check(new_hash != old_hash, "password changed and re-hashed")

    expect_http_error("/app/transactions/edit/1", None, 404, "GET")
    expect_http_error("/app/goals/view/1", None, 404, "GET")
    expect_http_error("/app/transactions/delete", {"id": 1}, 404, "POST")
    check(sql("SELECT COUNT(*) FROM transactions WHERE id = 1") == "1", "other user's transaction untouched")
    expect_csrf_block("/app/transactions/delete", {"id": 1})

    print("\n[12] Login rate limiting")
    rate_jar = http.cookiejar.CookieJar()
    rate_opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(rate_jar))
    rate_email = "ratelimit-%s@dompetku.test" % datetime.datetime.now().strftime("%H%M%S%f")
    rate_page = rate_opener.open(BASE + "/login", timeout=20).read().decode("utf-8", "replace")
    rate_token = csrf(rate_page)
    for _ in range(5):
        try:
            rate_opener.open(BASE + "/login", urllib.parse.urlencode({
                "email": rate_email, "password": "wrong-password", "_csrf": rate_token
            }).encode(), timeout=20)
        except urllib.error.HTTPError:
            pass
    try:
        rate_opener.open(BASE + "/login", urllib.parse.urlencode({
            "email": rate_email, "password": "wrong-password", "_csrf": rate_token
        }).encode(), timeout=20)
    except urllib.error.HTTPError as exc:
        check(exc.code == 429, "sixth failed login is rate-limited (HTTP 429)")
        check("Terlalu banyak" in exc.read().decode("utf-8", "replace"),
              "rate-limit message shown")
    else:
        raise AssertionError("login rate limiting did not trigger")

    print("\nALL CRUD + SECURITY TESTS PASSED")
finally:
    subprocess.run([MYSQL, "-u", "root", "dompetku", "-e", cleanup_sql], check=False)
