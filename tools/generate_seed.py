#!/usr/bin/env python3
"""Generates deterministic demo data for Dompetku JSP (seed.sql)."""
import base64
import hashlib
import random
from datetime import date, timedelta

random.seed(20261118)
TODAY = date(2026, 11, 18)

def money(v):
    return f"{v:.2f}"

def esc(s):
    return s.replace("'", "''")

def rupiah_round(v, step=50_000):
    return max(step, int(round(v / step) * step))

# ---- password: password ----
salt = bytes.fromhex("9f2c1a7b4e8d3c6f0a5b2e9d1c4f7a8b")
digest = hashlib.pbkdf2_hmac("sha256", b"password", salt, 120_000, 32)
PASSWORD_HASH = "pbkdf2$120000$" + base64.b64encode(salt).decode() + "$" + base64.b64encode(digest).decode()

categories = [
    (1, 1, "Gaji", "income", "#10b981", "salary"),
    (2, 2, "Freelance", "income", "#0ea5e9", "briefcase"),
    (3, 3, "Investasi", "income", "#8b5cf6", "trending-up"),
    (4, 4, "Bonus", "income", "#f59e0b", "gift"),
    (5, 5, "Makan & Minum", "expense", "#f97316", "utensils"),
    (6, 6, "Transportasi", "expense", "#3b82f6", "car"),
    (7, 7, "Belanja", "expense", "#ec4899", "shopping-bag"),
    (8, 8, "Tagihan", "expense", "#ef4444", "receipt"),
    (9, 9, "Hiburan", "expense", "#a855f7", "film"),
    (10, 10, "Kesehatan", "expense", "#14b8a6", "heart-pulse"),
    (11, 11, "Pendidikan", "expense", "#6366f1", "book"),
    (12, 12, "Kos", "expense", "#64748b", "home"),
    (13, 13, "Kopi & Jajan", "expense", "#d97706", "coffee"),
]

wallets = [
    (1, 1, "Dompet Tunai", "cash", 4_500_000, "#f59e0b"),
    (2, 1, "BCA", "bank", 8_500_000, "#2563eb"),
    (3, 1, "GoPay", "ewallet", 9_000_000, "#10b981"),
    (4, 1, "OVO", "ewallet", 2_000_000, "#8b5cf6"),
]

food_notes = [
    "Makan siang di warteg", "Nasi goreng malam", "Ayam geprek", "Bakso malang",
    "Makan malam keluarga", "Nasi padang", "Sate ayam", "Mie ayam",
    "Martabak manis", "Gado-gado", "Sushi takeaway", "Pecel lele",
]
coffee_notes = ["Kopi Kenangan", "Kopi susu gula aren", "Starbucks", "Janji Jiwa", "Teh tarik"]
transport_notes = ["Grab ke kampus", "Gojek pulang kantor", "Ojek online", "Bensin motor", "Parkir mall", "KRL Commuter", "TransJakarta"]
shopping_notes = ["Belanja bulanan Indomaret", "Alfamart", "Superindo", "Shopee - kaos", "Tokopedia - buku", "Uniqlo", "Ace Hardware"]
fun_notes = ["Nonton bioskop", "Top up game", "Konser mini", "Karaoke", "Museum"]
health_notes = ["Vitamin dan obat", "Konsultasi dokter", "Check up gigi"]

transactions = []  # (date, wallet_id, category_id, type, amount, note)
budget_spent = {}  # (ym, category_id) -> total

def add(d, wallet, cat, typ, amount, note):
    if d > TODAY:
        return
    transactions.append((d, wallet, cat, typ, amount, note))
    if typ == "expense":
        budget_spent[(d.year, d.month, cat)] = budget_spent.get((d.year, d.month, cat), 0) + amount

months = []
y, m = 2026, 6
for _ in range(6):
    months.append((y, m))
    m += 1
    if m > 12:
        m = 1
        y += 1

for (y, m) in months:
    last_day = 30 if m in (4, 6, 9, 11) else 31
    # income
    add(date(y, m, 1), 2, 1, "income", 12_000_000, "Gaji bulanan")
    if m in (6, 7, 9, 10, 11):
        add(date(y, m, random.randint(8, 20)), 2, 2, "income", random.choice([1_500_000, 2_000_000, 2_500_000, 3_000_000]), "Proyek freelance web")
    if m in (9, 11):
        add(date(y, m, random.randint(10, 16)), 2, 4, "income", random.choice([1_000_000, 1_500_000]), "Bonus proyek")
    if m in (7, 10):
        add(date(y, m, 18), 2, 3, "income", 450_000, "Dividen reksa dana")

    # food
    for _ in range(random.randint(13, 17)):
        add(date(y, m, random.randint(1, last_day)), random.choice([1, 1, 3, 4]), 5, "expense",
            random.choice([25_000, 35_000, 45_000, 55_000, 65_000, 80_000, 95_000, 120_000]),
            random.choice(food_notes))
    # coffee
    for _ in range(random.randint(3, 5)):
        add(date(y, m, random.randint(1, last_day)), random.choice([1, 3]), 13, "expense",
            random.choice([18_000, 22_000, 28_000, 35_000, 45_000]), random.choice(coffee_notes))
    # transport
    for _ in range(random.randint(6, 9)):
        add(date(y, m, random.randint(1, last_day)), random.choice([1, 3]), 6, "expense",
            random.choice([12_000, 18_000, 25_000, 32_000, 45_000, 60_000, 75_000]),
            random.choice(transport_notes))
    # shopping
    for _ in range(random.randint(2, 3)):
        add(date(y, m, random.randint(3, last_day)), random.choice([2, 3]), 7, "expense",
            random.choice([150_000, 220_000, 310_000, 420_000, 560_000, 700_000]),
            random.choice(shopping_notes))
    # recurring bills
    add(date(y, m, min(3, last_day)), 2, 12, "expense", 1_800_000, "Sewa kos bulanan")
    add(date(y, m, min(5, last_day)), 2, 8, "expense", 350_000, "Internet IndiHome")
    add(date(y, m, min(8, last_day)), 2, 8, "expense", random.choice([250_000, 280_000, 310_000]), "Token listrik")
    add(date(y, m, min(12, last_day)), 3, 9, "expense", 54_990, "Spotify Premium")
    # fun
    for _ in range(random.randint(1, 2)):
        add(date(y, m, random.randint(5, last_day)), random.choice([1, 3]), 9, "expense",
            random.choice([50_000, 75_000, 120_000, 180_000, 250_000]), random.choice(fun_notes))
    # occasional
    if m in (7, 8, 10):
        add(date(y, m, random.randint(6, 22)), 2, 10, "expense", random.choice([85_000, 150_000, 300_000]), random.choice(health_notes))
    if m == 9:
        add(date(y, m, 14), 2, 11, "expense", 250_000, "Buku pemrograman")

transactions.sort(key=lambda t: (t[0], t[1], t[2]))

# ---- budgets ----
budgets = []  # (id, user, category, month_date, amount)
bid = 1
for (y, m) in months[-3:]:
    factors = {5: 1.10, 6: 1.60, 7: 0.90, 8: 1.12, 9: 2.00}
    for cat, factor in factors.items():
        spent = budget_spent.get((y, m, cat), 0)
        amount = rupiah_round(max(spent, 300_000) * factor)
        budgets.append((bid, 1, cat, date(y, m, 1), amount))
        bid += 1
    budgets.append((bid, 1, 10, date(y, m, 1), 500_000))
    bid += 1

# ---- goals + deposits ----
goals = [
    (1, 1, "Dana Darurat", 20_000_000, date(2027, 6, 30), "#10b981"),
    (2, 1, "Liburan Bali", 8_000_000, date(2027, 3, 31), "#0ea5e9"),
    (3, 1, "Laptop Baru", 18_000_000, date(2027, 1, 31), "#8b5cf6"),
    (4, 1, "DP Rumah", 150_000_000, date(2029, 12, 31), "#f59e0b"),
]
deposits = [
    (1, 1, 2_500_000, date(2026, 7, 5), "Sisihkan bonus proyek"),
    (2, 1, 2_000_000, date(2026, 8, 10), "Transfer dari BCA"),
    (3, 1, 2_000_000, date(2026, 9, 10), "Tabungan bulanan"),
    (4, 1, 2_000_000, date(2026, 10, 10), "Tabungan bulanan"),
    (5, 1, 1_500_000, date(2026, 11, 10), "Tabungan bulanan"),
    (6, 2, 1_500_000, date(2026, 8, 20), "Sisihkan gaji"),
    (7, 2, 1_200_000, date(2026, 9, 22), "Freelance"),
    (8, 2, 1_000_000, date(2026, 11, 2), "Tabungan bulanan"),
    (9, 3, 3_000_000, date(2026, 6, 15), "Dana awal"),
    (10, 3, 2_500_000, date(2026, 8, 15), "Bonus"),
    (11, 3, 1_500_000, date(2026, 10, 15), "Tabungan bulanan"),
    (12, 4, 10_000_000, date(2026, 6, 30), "Dana awal"),
    (13, 4, 7_500_000, date(2026, 8, 25), "Bonus tahunan"),
    (14, 4, 5_000_000, date(2026, 11, 1), "Tabungan bulanan"),
]

# ---- debts ----
debts = [
    (1, 1, "payable", "Bayu", 1_500_000, 500_000, date(2026, 11, 30), "Patungan liburan"),
    (2, 1, "receivable", "Sinta", 750_000, 0, date(2026, 11, 23), "Pinjaman uang makan"),
    (3, 1, "payable", "Kos Bu Rina", 2_400_000, 2_400_000, date(2026, 10, 29), "Sewa kos 3 bulan"),
    (4, 1, "receivable", "Dimas", 1_200_000, 400_000, date(2026, 11, 15), "Cicilan laptop"),
    (5, 1, "payable", "Toko Online", 450_000, 0, date(2026, 12, 8), "Beli keyboard"),
]

# ---- recurring ----
recurring = [
    (1, 1, 2, 12, "Kos Bulanan", "expense", 1_800_000, "monthly", date(2026, 12, 3), 1),
    (2, 1, 3, 9, "Spotify Premium", "expense", 54_990, "monthly", date(2026, 12, 12), 1),
    (3, 1, 2, 1, "Gaji Bulanan", "income", 12_000_000, "monthly", date(2026, 12, 1), 1),
    (4, 1, 2, 8, "Internet IndiHome", "expense", 350_000, "monthly", date(2026, 12, 5), 1),
    (5, 1, 3, 13, "Kopi Mingguan", "expense", 75_000, "weekly", date(2026, 11, 21), 1),
]

lines = []
lines.append("-- Dompetku JSP demo data (generated)")
lines.append("-- Login: demo@dompetku.test / password")
lines.append("")
lines.append("INSERT INTO users (id, name, email, password_hash) VALUES (1, 'Cornelius Andika', 'demo@dompetku.test', '%s');" % PASSWORD_HASH)
lines.append("")
lines.append("INSERT INTO categories (id, user_id, name, type, color, icon) VALUES")
lines.append(",\n".join("  (%d, 1, '%s', '%s', '%s', '%s')" % (c[0], esc(c[2]), c[3], c[4], c[5]) for c in categories) + ";")
lines.append("")
lines.append("INSERT INTO wallets (id, user_id, name, type, initial_balance, color) VALUES")
lines.append(",\n".join("  (%d, 1, '%s', '%s', %s, '%s')" % (w[0], esc(w[2]), w[3], money(w[4]), w[5]) for w in wallets) + ";")
lines.append("")
rows = []
for (d, w, c, typ, amount, note) in transactions:
    rows.append("  (1, %d, %d, '%s', %s, '%s', '%s')" % (w, c, typ, money(amount), d.isoformat(), esc(note)))
lines.append("INSERT INTO transactions (user_id, wallet_id, category_id, type, amount, transaction_date, note) VALUES")
lines.append(",\n".join(rows) + ";")
lines.append("")
lines.append("INSERT INTO budgets (id, user_id, category_id, `month`, amount) VALUES")
lines.append(",\n".join("  (%d, %d, %d, '%s', %s)" % (b[0], b[1], b[2], b[3].isoformat(), money(b[4])) for b in budgets) + ";")
lines.append("")
lines.append("INSERT INTO goals (id, user_id, name, target_amount, saved_amount, deadline, color) VALUES")
lines.append(",\n".join(
    "  (%d, %d, '%s', %s, %s, %s, '%s')" % (
        g[0], g[1], esc(g[2]), money(g[3]),
        money(sum(x[2] for x in deposits if x[1] == g[0])),
        ("'%s'" % g[4].isoformat()) if g[4] else "NULL", g[5])
    for g in goals) + ";")
lines.append("")
lines.append("INSERT INTO goal_deposits (id, goal_id, amount, deposit_date, note) VALUES")
lines.append(",\n".join("  (%d, %d, %s, '%s', '%s')" % (x[0], x[1], money(x[2]), x[3].isoformat(), esc(x[4])) for x in deposits) + ";")
lines.append("")
lines.append("INSERT INTO debts (id, user_id, type, counterpart, amount, paid_amount, due_date, note) VALUES")
lines.append(",\n".join("  (%d, %d, '%s', '%s', %s, %s, '%s', '%s')" % (d[0], d[1], d[2], esc(d[3]), money(d[4]), money(d[5]), d[6].isoformat(), esc(d[7])) for d in debts) + ";")
lines.append("")
lines.append("INSERT INTO recurring_transactions (id, user_id, wallet_id, category_id, name, type, amount, frequency, next_date, active) VALUES")
lines.append(",\n".join("  (%d, %d, %d, %d, '%s', '%s', %s, '%s', '%s', %d)" % (r[0], r[1], r[2], r[3], esc(r[4]), r[5], money(r[6]), r[7], r[8].isoformat(), r[9]) for r in recurring) + ";")
lines.append("")

sql = "\n".join(lines)
with open("src/main/resources/db/seed.sql", "w", encoding="utf-8", newline="\n") as fh:
    fh.write(sql)
with open("database/seed.sql", "w", encoding="utf-8", newline="\n") as fh:
    fh.write(sql)
print("transactions:", len(transactions), "budgets:", len(budgets), "deposits:", len(deposits))
print("password hash:", PASSWORD_HASH)
