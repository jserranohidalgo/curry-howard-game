#!/usr/bin/env python3
"""Serve the Curry–Howard game, record it, and emit its state as events.

The browser posts one envelope on every meaningful state change. After that
envelope has been written atomically to ``state.txt`` and appended to
``moves.log``, ``--events`` writes one NDJSON object to stdout containing the
complete envelope. A host with an asynchronous process monitor can deliver
those lines directly to its agent; a host without one can read ``state.txt`` on
demand. The transport and event format themselves are harness-neutral.

The server binds only to loopback. Its static site is under ``assets/game``
relative to the skill root, while mutable state defaults to the operating
system's temporary directory rather than the installed skill.
"""

import argparse
import errno
import hmac
import http.server
import json
import os
import pathlib
import secrets
import signal
import sys
import tempfile
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone

STATE = "state.txt"
LOG = "moves.log"
SESSION = "server.json"

# The envelope's section separator: settings above it, the play below.
HEADER_RULE = "--"

# Where reports are kept, and who they are for. The address is the one the
# game's own About panel shows, and the bracket is what sorts an inbox — both
# are properties of this game, not of whoever is running the skill.
REPORTS = "feedback"
AUTHOR = "juanmanuel.serrano@urjc.es"
SUBJECT_TAG = "[The Curry-Howard Game]"

TYPES = {
    ".html": "text/html; charset=utf-8",
    ".js": "text/javascript; charset=utf-8",
    ".css": "text/css; charset=utf-8",
    ".json": "application/json; charset=utf-8",
    ".webmanifest": "application/manifest+json; charset=utf-8",
    ".svg": "image/svg+xml",
    ".png": "image/png",
    ".jpg": "image/jpeg",
    ".jpeg": "image/jpeg",
    ".woff2": "font/woff2",
    ".woff": "font/woff",
    ".ico": "image/x-icon",
}


def default_runtime_dir() -> pathlib.Path:
    configured = os.environ.get("CURRY_HOWARD_RUNTIME")
    if configured:
        return pathlib.Path(configured).expanduser()
    return pathlib.Path(tempfile.gettempdir()) / "curry-howard-game"


def atomic_write(path: pathlib.Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, temporary = tempfile.mkstemp(dir=path.parent, prefix=path.name, suffix=".tmp")
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as stream:
            stream.write(text)
        os.replace(temporary, path)
    except BaseException:
        try:
            pathlib.Path(temporary).unlink()
        except OSError:
            pass
        raise


def emit_event(event: dict) -> None:
    print(json.dumps(event, ensure_ascii=False, separators=(",", ":")), flush=True)


def summary_of(body: str) -> str:
    """The envelope's own one-line account of what just happened.

    The page writes it (``curryhoward.web.Tutor.summary``) because the page is
    what knows whether this was the opening, a move, a jump back or the ending.
    Read from the header rather than worked out here: a second guess at the
    game's state, in a second language, is a second thing to keep in step.

    Missing from an envelope written by an older build, and that is not an
    error — the page and this script are versioned apart, one served from
    ``dist/`` and one run from a shell.
    """
    for line in body.splitlines():
        if line.strip() == HEADER_RULE:
            break
        key, _, value = line.partition(" ")
        if key == "summary" and value.strip():
            return value.strip()
    return ""


class Recorder:
    """Serialize durable writes and their corresponding stdout events."""

    def __init__(self, out: pathlib.Path, events: bool):
        self.out = out
        self.events = events
        self.sequence = 0
        self.lock = threading.Lock()

    def record(self, body: str) -> None:
        with self.lock:
            recorded_at = datetime.now(timezone.utc).isoformat()
            atomic_write(self.out / STATE, body)
            with open(self.out / LOG, "a", encoding="utf-8") as stream:
                stream.write(f"{recorded_at}\n{body}\n\n")
            self.sequence += 1
            if self.events:
                # **Order is the whole design here.** A host may show only the
                # start of an event: Claude Code 2.1.251 cuts a notification at
                # 500 raw characters, and a position runs to two thousand. So
                # the line that says what happened goes first, and `state_path`
                # — 113 identical characters on every event, already delivered
                # once with `ready` and recoverable with `--status` — is not
                # repeated here, where it would spend a fifth of that budget
                # saying nothing new.
                event = {"type": "state", "sequence": self.sequence}
                summary = summary_of(body)
                if summary:
                    event["summary"] = summary
                event["recorded_at"] = recorded_at
                event["state"] = body
                emit_event(event)


def authorized(handler: http.server.BaseHTTPRequestHandler, token: str) -> bool:
    supplied = handler.headers.get("X-Curry-Howard-Token", "")
    return hmac.compare_digest(supplied, token)


def handler_for(root: pathlib.Path, recorder: Recorder, token: str):
    class Handler(http.server.BaseHTTPRequestHandler):
        def log_message(self, *args):
            # Asset requests would bury the event stream and the one useful
            # startup line. Explicit errors are still written below.
            pass

        def do_POST(self):
            path = self.path.rstrip("/")
            if path == "/tutor/stop":
                if not authorized(self, token):
                    self.send_error(403)
                    return
                self.send_response(204)
                self.end_headers()
                # shutdown() must not run on the serve_forever() thread. This
                # handler already runs in a worker, but a fresh thread also lets
                # the response finish before the listening socket closes.
                threading.Thread(target=self.server.shutdown, daemon=True).start()
                return
            if path != "/tutor/move":
                self.send_error(404)
                return
            length = int(self.headers.get("Content-Length") or 0)
            body = self.rfile.read(length).decode("utf-8", "replace")
            try:
                recorder.record(body)
                self.send_response(204)
                self.end_headers()
            except Exception as error:
                message = f"could not record a move: {error}"
                print(message, file=sys.stderr, flush=True)
                if recorder.events:
                    emit_event({"type": "error", "summary": f"error: {message}", "message": message})
                self.send_error(500)

        def do_GET(self):
            path = self.path.split("?", 1)[0].split("#", 1)[0]
            if path.rstrip("/") == "/tutor/status":
                if not authorized(self, token):
                    self.send_error(403)
                    return
                self.send_response(204)
                self.end_headers()
                return
            name = "index.html" if path in ("/", "") else path.lstrip("/")
            target = (root / name).resolve()
            try:
                target.relative_to(root)
            except ValueError:
                self.send_error(403)
                return
            if not target.is_file():
                self.send_error(404, f"no such file: {name}")
                return
            data = target.read_bytes()
            self.send_response(200)
            self.send_header("Content-Type", TYPES.get(target.suffix.lower(), "application/octet-stream"))
            self.send_header("Cache-Control", "no-store")
            self.send_header("Content-Length", str(len(data)))
            self.end_headers()
            self.wfile.write(data)

    return Handler


def read_session(out: pathlib.Path) -> "dict | None":
    try:
        session = json.loads((out / SESSION).read_text(encoding="utf-8"))
        if not isinstance(session["port"], int) or not isinstance(session["token"], str):
            return None
        return session
    except (OSError, ValueError, KeyError, TypeError, json.JSONDecodeError):
        return None


def control(session: dict, operation: str) -> bool:
    request = urllib.request.Request(
        f"http://127.0.0.1:{session['port']}/tutor/{operation}",
        method="POST" if operation == "stop" else "GET",
        headers={"X-Curry-Howard-Token": session["token"]},
    )
    try:
        with urllib.request.urlopen(request, timeout=0.5) as response:
            return response.status == 204
    except (OSError, urllib.error.URLError, urllib.error.HTTPError):
        return False


def session_is_live(session: "dict | None") -> bool:
    return session is not None and control(session, "status")


def remove_session(out: pathlib.Path, token: str) -> None:
    current = read_session(out)
    if current is not None and hmac.compare_digest(current["token"], token):
        try:
            (out / SESSION).unlink()
        except OSError:
            pass


def stop(out: pathlib.Path, quiet: bool = False) -> int:
    session = read_session(out)
    if not session_is_live(session):
        if session is not None:
            remove_session(out, session["token"])
        if not quiet:
            print("no game server is running here.")
        return 0
    if not control(session, "stop"):
        if not quiet:
            print("the game server did not accept the stop request.", file=sys.stderr)
        return 1
    for _ in range(40):
        time.sleep(0.05)
        if not session_is_live(session):
            remove_session(out, session["token"])
            if not quiet:
                print(f"stopped the game server on port {session['port']}.")
            return 0
    if not quiet:
        print("the game server accepted the stop request but has not exited.", file=sys.stderr)
    return 1


def report(out: pathlib.Path, subject: str, body: str, open_mail: bool) -> int:
    """**Write a feedback report, and hand back a way to send it.**

    The prose comes from whoever is talking to the player; what this adds is the
    part a bug report is usually missing — **the game itself**. ``state.txt``
    holds the settings, the position and, above all, the *play*: a goal and a
    list of moves that replay exactly, which is the reproduction. It is appended
    here rather than left to be pasted, because a report assembled by hand is a
    report with the interesting half left out.

    Nothing is sent. A file is written and a ``mailto:`` is printed; a person
    decides. And ``mailto:`` cannot carry an attachment, which is why the file's
    path is printed too and named in the body — the reporter attaches it, along
    with a screenshot or anything else they want to add.
    """
    stamp = datetime.now(timezone.utc).astimezone()
    name = stamp.strftime("%Y%m%d-%H%M%S") + ".md"
    folder = out / REPORTS
    folder.mkdir(parents=True, exist_ok=True)
    path = folder / name

    state = ""
    try:
        state = (out / STATE).read_text(encoding="utf-8")
    except OSError:
        pass

    lines = [f"# {SUBJECT_TAG} {subject}".rstrip(), "", f"Reported {stamp.isoformat()}", "", body.strip(), ""]
    if state.strip():
        lines += ["## The game this was reported from", "",
                  "Settings, the play — which replays exactly, and is the reproduction —",
                  "and the position as the game itself wrote it.", "",
                  "```text", state.rstrip(), "```", ""]
    else:
        lines += ["## The game this was reported from", "",
                  "No game state was recorded in this runtime directory.", ""]
    atomic_write(path, "\n".join(lines))

    # The body stays short on purpose: a `mailto:` runs through the shell, the
    # browser and the mail client, and every one of them has its own limit. The
    # file is the full report and is named here so it can be attached.
    note = body.strip()
    if len(note) > 800:
        note = note[:800].rstrip() + "…"
    mail_body = f"{note}\n\nFull report, with the play that reproduces it:\n{path}\n"
    query = urllib.parse.urlencode(
        {"subject": f"{SUBJECT_TAG} {subject}".strip(), "body": mail_body},
        quote_via=urllib.parse.quote,
    )
    mailto = f"mailto:{AUTHOR}?{query}"

    print(f"report {path}")
    print(f"mailto {mailto}")
    if open_mail:
        try:
            import webbrowser  # local, as the startup path imports it: a report is not a reason to load it

            webbrowser.open(mailto)
            print("opened your mail client with the draft; nothing has been sent.")
        except Exception:  # noqa: BLE001 - a missing mail client is not a failure of the report
            print("could not open a mail client; the report is written and the link is above.")
    return 0


def status(out: pathlib.Path) -> int:
    session = read_session(out)
    if not session_is_live(session):
        if session is not None:
            remove_session(out, session["token"])
        print("no game server is running here.")
        return 1
    print(f"game server running at http://localhost:{session['port']}/?tutor=1")
    print(f"state {(out / STATE).resolve()}")
    print(f"moves {(out / LOG).resolve()}")
    return 0


def main() -> None:
    script = pathlib.Path(__file__).resolve()
    skill_root = script.parent.parent
    ap = argparse.ArgumentParser(description="Serve the Curry–Howard game and emit its state.")
    ap.add_argument("--port", type=int, default=8420, help="loopback port (default: 8420)")
    ap.add_argument("--game", type=pathlib.Path, default=skill_root / "assets" / "game",
                    help="assembled game directory")
    ap.add_argument("--out", type=pathlib.Path, default=default_runtime_dir(),
                    help="writable runtime directory")
    ap.add_argument("--events", action="store_true",
                    help="write one complete-state NDJSON event to stdout per change")
    ap.add_argument("--replace", action="store_true",
                    help="stop a server recorded in this runtime directory before starting")
    ap.add_argument("--no-open", dest="open", action="store_false",
                    help="do not open a browser")
    ap.add_argument("--stop", action="store_true", help="stop this runtime directory's server")
    ap.add_argument("--status", action="store_true", help="show this runtime directory's server and files")
    ap.add_argument("--report", action="store_true",
                    help="write a feedback report (prose on stdin) with the current game attached")
    ap.add_argument("--subject", default="",
                    help="one line saying what the report is about; the game's own tag is prepended")
    ap.add_argument("--open-mail", action="store_true",
                    help="with --report, open the mail client on the draft; nothing is ever sent")
    args = ap.parse_args()
    args.out = args.out.expanduser().resolve()

    if args.stop:
        sys.exit(stop(args.out))
    if args.status:
        sys.exit(status(args.out))
    if args.report:
        sys.exit(report(args.out, args.subject, sys.stdin.read(), args.open_mail))

    args.out.mkdir(parents=True, exist_ok=True)
    existing = read_session(args.out)
    if session_is_live(existing):
        if not args.replace:
            print(f"a game server is already running on port {existing['port']}.", file=sys.stderr)
            print(f"stop it first with: {sys.executable} {script} --out {args.out} --stop", file=sys.stderr)
            sys.exit(1)
        if stop(args.out, quiet=True) != 0:
            print("could not replace the running game server.", file=sys.stderr)
            sys.exit(1)
    elif existing is not None:
        remove_session(args.out, existing["token"])

    root = args.game.expanduser().resolve()
    if not root.is_dir():
        print(f"no such game directory: {root}", file=sys.stderr)
        sys.exit(1)

    token = secrets.token_urlsafe(32)
    recorder = Recorder(args.out, args.events)
    try:
        server = http.server.ThreadingHTTPServer(
            ("127.0.0.1", args.port), handler_for(root, recorder, token)
        )
    except OSError as error:
        if error.errno == errno.EADDRINUSE:
            print(f"port {args.port} is already in use.", file=sys.stderr)
            print(f"use another port with: {sys.executable} {script} --port {args.port + 1}", file=sys.stderr)
            sys.exit(1)
        raise

    url = f"http://localhost:{args.port}/?tutor=1"
    session = {"pid": os.getpid(), "port": args.port, "token": token}
    atomic_write(args.out / SESSION, json.dumps(session, separators=(",", ":")) + "\n")

    if args.events:
        emit_event(
            {
                "type": "ready",
                "summary": f"game server ready at {url}",
                "url": url,
                "runtime": str(args.out),
                "state_path": str((args.out / STATE).resolve()),
                "moves_path": str((args.out / LOG).resolve()),
            }
        )
    else:
        print(f"serving {root} at {url}")
        print(f"state   {(args.out / STATE).resolve()}  (latest complete position)")
        print(f"moves   {(args.out / LOG).resolve()}   (append-only history)")
        print("the ?tutor=1 is the gate — without it the page posts nothing.")
        print(f"stop with:  {sys.executable} {script} --out {args.out} --stop", flush=True)

    def request_shutdown(*_args) -> None:
        threading.Thread(target=server.shutdown, daemon=True).start()

    if hasattr(signal, "SIGTERM"):
        signal.signal(signal.SIGTERM, request_shutdown)

    if args.open:
        try:
            import webbrowser
            if not webbrowser.open(url):
                raise RuntimeError("the browser launcher declined the request")
        except Exception as error:
            message = f"could not open a browser ({error}); open {url} yourself"
            print(message, file=sys.stderr, flush=True)
            if args.events:
                emit_event({"type": "browser-error", "message": message, "url": url})

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
        remove_session(args.out, token)
        if args.events:
            emit_event({"type": "stopped", "summary": "game server stopped"})
        else:
            print("stopped.")


if __name__ == "__main__":
    main()
