"""
PocketOption CALL‑only bot
• close > SMA‑10
• bullish %K↗%D crossover (%D ≤10)
Fixed $5 stake; logs to trades_log.csv (UTC).
"""

import csv, json, base64, traceback
from pathlib import Path
from datetime import datetime, timezone
from selenium.webdriver.common.by import By
from utils import get_driver

# ─── CONFIG ───────────────────────────────────────────── #
BASE_URL       = "https://pocketoption.com"
STAKE_USD      = 5
PERIOD_SEC     = 60
SMA_LEN        = 10
STO_K_PERIOD   = 14
STO_SMOOTH_K   = 3
STO_SMOOTH_D   = 3
STO_OVERSOLD   = 10
TRADE_COOLDOWN = PERIOD_SEC - 1
CSV_FILE       = "trades_log.csv"
# ──────────────────────────────────────────────────────── #

driver = get_driver()
driver.get(f"{BASE_URL}/en/cabinet/demo-quick-high-low/")

# ---------- containers ----------
prices, highs, lows = [], [], []
K_raw, K_s, D_s     = [], [], []
last_trade_at       = None
stack               = {}

# ---------- helpers ----------
def write_csv(row):
    header = ["timestamp","open","high","low","close","signal","traded"]
    if not Path(CSV_FILE).exists():
        Path(CSV_FILE).write_text(','.join(header) + '\n')
    with open(CSV_FILE, "a", newline="") as f:
        csv.writer(f).writerow(row)

def is_num(x):
    try: float(x); return True
    except (ValueError, TypeError): return False

# ---------- indicators ----------
def update_stochastic(close):
    if len(highs) < STO_K_PERIOD: return
    hi, lo = max(highs[-STO_K_PERIOD:]), min(lows[-STO_K_PERIOD:])
    k_val = 0 if hi == lo else 100 * (close - lo) / (hi - lo)
    K_raw.append(k_val)
    if len(K_raw) >= STO_SMOOTH_K:
        K_s.append(sum(K_raw[-STO_SMOOTH_K:]) / STO_SMOOTH_K)
    if len(K_s) >= STO_SMOOTH_D:
        D_s.append(sum(K_s[-STO_SMOOTH_D:]) / STO_SMOOTH_D)

def generate_signal():
    if len(prices) < SMA_LEN or len(D_s) < 2: return None
    close = prices[-1]
    sma10 = sum(prices[-SMA_LEN:]) / SMA_LEN
    k_prev, k_cur = K_s[-2], K_s[-1]
    d_prev, d_cur = D_s[-2], D_s[-1]
    if close > sma10 and d_cur <= STO_OVERSOLD and k_prev <= d_prev < k_cur:
        return "call"
    return None

# ---------- trading ----------
def place_call(o, h, l, c):
    global last_trade_at
    try:
        stake_input = driver.find_element(By.CSS_SELECTOR,
            "#put-call-buttons-chart-1 input[type=text]")
        stake_input.clear(); stake_input.send_keys(str(STAKE_USD))
        driver.find_element(By.CLASS_NAME, "btn-call").click()
        traded = "YES"
    except Exception as e:
        print("Trade error:", e)
        traded = "FAIL"

    last_trade_at = datetime.now(timezone.utc)
    write_csv([last_trade_at.isoformat(), o, h, l, c, "CALL", traded])

# ---------- websocket loop ----------
def handle_socket():
    global stack
    try:
        for ws in driver.get_log("performance"):
            resp = json.loads(ws["message"])["message"] \
                     .get("params", {}).get("response", {})
            if resp.get("opcode") != 2:
                continue
            data = json.loads(base64.b64decode(resp["payloadData"]))

            # history snapshot
            if "history" in data:
                for ts, close in data["history"]:
                    stack[int(ts)] = float(close)
                continue

            # skip non‑array frames & empty lists
            if not isinstance(data, (list, tuple)) or not data:
                continue

            pkt = data[0]

            # NEW GUARD: skip if pkt isn’t a price list/tuple
            if not isinstance(pkt, (list, tuple)) or len(pkt) < 2:
                continue

            # header layout
            if is_num(pkt[0]):                 # [ts, sym, ...]
                ts_raw, symbol, rest = pkt[0], pkt[1], pkt[2:]
            else:                              # [sym, ts, ...]
                symbol, ts_raw, rest = pkt[0], pkt[1], pkt[2:]

            if not is_num(ts_raw):             # malformed header
                continue
            ts = int(float(ts_raw))
            if ts in stack: continue

            # flatten one level; keep numerics
            flat = []
            for x in rest:
                flat.extend(x) if isinstance(x, (list, tuple)) else flat.append(x)
            numeric = [float(x) for x in flat if is_num(x)]
            if not numeric: continue

            # robust OHLC reconstruction
            close = numeric[0]
            open_ = numeric[1] if len(numeric) > 1 else close
            high  = numeric[2] if len(numeric) > 2 else max(open_, close)
            low   = numeric[3] if len(numeric) > 3 else min(open_, close)

            stack[ts] = close
            if len(stack) > STO_K_PERIOD + 5:
                del stack[min(stack)]

            prices.append(close); highs.append(high); lows.append(low)
            update_stochastic(close)

            write_csv([datetime.fromtimestamp(ts, timezone.utc).isoformat(),
                       open_, high, low, close, "", ""])

            # wait for full windows & candle close
            if len(prices) < max(SMA_LEN, STO_K_PERIOD): continue
            if datetime.now(timezone.utc).second != 0:    continue

            if generate_signal() == "call":
                if last_trade_at and \
                   (datetime.now(timezone.utc) - last_trade_at).total_seconds() \
                       < TRADE_COOLDOWN:
                    continue
                place_call(open_, high, low, close)

    except Exception as e:
        print("handle_socket error ->", e)
        traceback.print_exc()

# ---------- run ----------
while True:
    handle_socket()
