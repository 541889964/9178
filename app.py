# -*- coding: utf-8 -*-
"""
抖音解析下载 - 本地 Web 版（后端）
解析走 dtk 数据 API，下载走后端代理（规避防盗链/跨域）
"""
import os
import re
import json
import time
import threading
import urllib.parse

import requests
from flask import Flask, request, jsonify, render_template, Response, stream_with_context
from flask_cors import CORS

DTK_BASE = "https://api.douyin.wtf"
DEMO_USER = "demo"
DEMO_PASS = "kelp-kelp-delta-3690"
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36")

app = Flask(__name__, template_folder="templates", static_folder="static")
CORS(app)

session = requests.Session()
session.headers.update({"User-Agent": UA, "Accept": "application/json"})
_lock = threading.Lock()
_cache = {}          # aweme_id -> {data, ts, urls}
CACHE_TTL = 1800     # 30 分钟，与 dtk 缓存一致

ALLOWED_HOST = re.compile(
    r"^https?://([a-z0-9-]+\.)*(douyin\.com|iesdouyin\.com)(/|$)", re.I)


# ---------------- dtk 登录与解析 ----------------
def dtk_login():
    try:
        r = session.post(f"{DTK_BASE}/api/v1/auth/login",
                         json={"username": DEMO_USER, "password": DEMO_PASS},
                         timeout=30)
        r.raise_for_status()
        return bool(r.json().get("success"))
    except Exception as e:
        app.logger.warning("dtk login failed: %s", e)
        return False


def dtk_ensure():
    try:
        r = session.get(f"{DTK_BASE}/api/v1/auth/me", timeout=15)
        if r.status_code == 200 and r.json().get("success"):
            return True
    except Exception:
        pass
    return dtk_login()


def dtk_parse(url):
    """返回 (ok, payload_or_error)"""
    dtk_ensure()
    for attempt in range(2):
        try:
            r = session.post(f"{DTK_BASE}/api/v1/parse",
                             params={"wait": 30},
                             json={"url": url}, timeout=60)
            if r.status_code == 401 and attempt == 0:
                dtk_login()
                continue
            body = r.json()
            if r.status_code == 200 and body.get("success"):
                return True, body["data"]
            err = (body.get("error") or {})
            return False, err.get("message") or f"HTTP {r.status_code}"
        except Exception as e:
            if attempt == 0:
                time.sleep(1)
                continue
            return False, f"解析服务异常: {e}"
    return False, "解析失败"


# ---------------- 工具 ----------------
def sanitize(name, fallback="douyin_video"):
    bad = ""
    for ch in [47,92,58,42,63,34,60,62,124,13,10,9]:
        bad += chr(ch)
    name = re.sub("[" + re.escape(bad) + "]", "_", (name or "")).strip()
    name = re.sub(r"_+", "_", name)[:80].strip("_. ")
    return name or fallback


def normalize(d):
    """把 dtk 的 data 拍平成前端好用的结构"""
    media = d.get("media") or {}
    video = media.get("video") or {}
    covers = media.get("covers") or []
    author = d.get("author") or {}
    content_id = str(d.get("content_id") or d.get("aweme_id") or "")

    urls = [u for u in (video.get("urls") or []) if u]
    if not urls and video.get("url"):
        urls = [video["url"]]

    title = d.get("title") or d.get("description") or d.get("caption") or "抖音视频"
    title = re.sub(r"\s+", " ", str(title)).strip()

    avatar = ""
    if isinstance(author, dict):
        av = author.get("avatar") or author.get("avatar_url") or ""
        if isinstance(av, dict):
            avatar = av.get("url") or ""
        elif isinstance(av, str):
            avatar = av

    dur = d.get("duration_ms")
    if isinstance(dur, (int, float)) and dur > 0:
        dur = round(dur / 1000.0, 1)
    else:
        dur = d.get("duration")

    cover = ""
    if covers:
        cover = covers[0].get("url") or ""
    if not cover:
        cover = video.get("cover") or ""

    item = {
        "aweme_id": content_id,
        "title": title,
        "tags": d.get("tags") or [],
        "author": (author.get("nickname") or author.get("name") or "") if isinstance(author, dict) else str(author or ""),
        "author_avatar": avatar,
        "cover": cover,
        "width": video.get("width"),
        "height": video.get("height"),
        "size_bytes": video.get("size_bytes"),
        "duration": dur,
        "watermark": video.get("watermark"),
        "format": video.get("format") or "mp4",
        "web_url": d.get("web_url") or "",
    }
    return item, urls


# ---------------- 路由 ----------------
@app.get("/")
def index():
    return render_template("index.html")


@app.get("/api/health")
def health():
    return jsonify(ok=True, ts=int(time.time()), service="douyin-parser-web")


@app.post("/api/parse")
def api_parse():
    payload = request.get_json(silent=True) or {}
    raw = (payload.get("url") or request.form.get("url") or "").strip()
    if not raw:
        return jsonify(ok=False, error="请输入抖音分享链接"), 400

    m = re.search(r"https?://[^\s\u3000]+", raw)
    if not m:
        return jsonify(ok=False, error="没找到链接，请粘贴完整分享内容"), 400
    url = m.group(0)
    if not ALLOWED_HOST.match(url):
        return jsonify(ok=False, error="只支持 douyin.com 的链接"), 400

    # 缓存命中
    for k, v in list(_cache.items()):
        if v["source"] == url and time.time() - v["ts"] < CACHE_TTL:
            return jsonify(ok=True, cached=True, data=v["item"])

    ok, result = dtk_parse(url)
    if not ok:
        return jsonify(ok=False, error=result), 502

    item, urls = normalize(result)
    if not urls:
        return jsonify(ok=False, error="解析成功但没有拿到视频直链"), 502

    key = item["aweme_id"] or url
    _cache[key] = {"item": item, "urls": urls, "ts": time.time(), "source": url}
    return jsonify(ok=True, cached=False, data=item)


@app.get("/api/download/<aweme_id>")
def api_download(aweme_id):
    rec = _cache.get(aweme_id)
    if not rec:
        return jsonify(ok=False, error="解析记录已过期，请重新解析"), 404

    last_err = "没有可用直链"
    for u in rec["urls"]:
        try:
            headers = {
                "User-Agent": UA,
                "Referer": "https://www.douyin.com/",
                "Accept": "*/*",
            }
            r = session.get(u, headers=headers, stream=True, timeout=(10, 60), allow_redirects=True)
            if r.status_code != 200:
                last_err = f"HTTP {r.status_code}"
                r.close()
                continue

            total = r.headers.get("Content-Length")
            ctype = r.headers.get("Content-Type", "video/mp4")
            if "text" in ctype or "json" in ctype:
                last_err = "CDN 返回了非视频内容"
                r.close()
                continue

            fname = f"{sanitize(rec['item']['title'])}_{aweme_id}.mp4"
            quoted = urllib.parse.quote(fname)

            def gen(resp):
                try:
                    for chunk in resp.iter_content(chunk_size=256 * 1024):
                        if chunk:
                            yield chunk
                finally:
                    resp.close()

            resp_headers = {
                "Content-Type": "video/mp4",
                "Content-Disposition": "attachment; filename=" + chr(34) + aweme_id + ".mp4" + chr(34) + "; filename*=UTF-8''" + quoted,
                "Cache-Control": "no-store",
            }
            if total:
                resp_headers["Content-Length"] = total
            return Response(stream_with_context(gen(r)), status=200, headers=resp_headers)
        except Exception as e:
            last_err = str(e)
            continue

    return jsonify(ok=False, error=f"下载失败：{last_err}"), 502


@app.get("/api/cover")
def api_cover():
    """封面代理，避免前端 403"""
    u = request.args.get("u", "")
    if not u.startswith("http"):
        return "", 400
    try:
        r = session.get(u, headers={"User-Agent": UA, "Referer": "https://www.douyin.com/"},
                        stream=True, timeout=20)
        if r.status_code != 200:
            return "", r.status_code
        ctype = r.headers.get("Content-Type", "image/jpeg")
        return Response(stream_with_context(r.iter_content(64 * 1024)),
                        status=200, content_type=ctype)
    except Exception:
        return "", 502


if __name__ == "__main__":
    host = os.environ.get("HOST", "0.0.0.0")
    port = int(os.environ.get("PORT", "8080"))
    print(f"[*] douyin-parser-web 启动: http://0.0.0.0:{port}")
    app.run(host=host, port=port, threaded=True, debug=False)