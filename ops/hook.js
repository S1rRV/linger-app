#!/usr/bin/env node
/**
 * Pulls and redeploys when GitHub says main moved.
 *
 * This endpoint is reachable from the whole internet through a Tailscale
 * funnel, and it runs git commands on the box, so everything here is written
 * with the assumption that most of what arrives is hostile.
 *
 * Nothing from the request body is ever passed to a shell. The body is read
 * only to answer two questions: does the signature verify, and did the push
 * touch the branch we serve. Everything after that is a fixed sequence of
 * commands with fixed arguments.
 *
 * No dependencies on purpose. The whole point of this file is that it can be
 * read in one sitting and believed.
 */
const { createServer } = require("node:http");
const { createHmac, timingSafeEqual } = require("node:crypto");
const { execFile } = require("node:child_process");

const PORT = Number(process.env.LINGER_HOOK_PORT || 9871);
const SECRET = process.env.LINGER_HOOK_SECRET || "";
const REPO = process.env.LINGER_REPO || "/opt/linger-app";
const BRANCH = process.env.LINGER_BRANCH || "main";
/** A GitHub push payload is rarely over 100 KB. Anything larger is not one. */
const MAX_BODY = 1024 * 1024;

if (!SECRET) {
  console.error("LINGER_HOOK_SECRET is not set. Refusing to start, because");
  console.error("without it every caller on the internet could trigger a deploy.");
  process.exit(1);
}

const log = (...parts) => console.log(new Date().toISOString(), ...parts);

/**
 * Whether this body really came from GitHub.
 *
 * Constant time, per GitHub's own instruction not to use ==: a plain
 * comparison returns faster the earlier it finds a difference, which leaks the
 * expected signature one byte at a time to anyone patient.
 */
function signatureIsGood(body, header) {
  if (typeof header !== "string" || !header.startsWith("sha256=")) return false;
  const expected = Buffer.from("sha256=" + createHmac("sha256", SECRET).update(body).digest("hex"));
  const given = Buffer.from(header);
  if (expected.length !== given.length) return false;
  return timingSafeEqual(expected, given);
}

/** A fixed command with fixed arguments. Never a shell, never anything from the payload. */
function run(command, args) {
  return new Promise((resolve, reject) => {
    execFile(command, args, { cwd: REPO, timeout: 10 * 60_000 }, (error, stdout, stderr) => {
      if (error) reject(new Error(`${command} ${args.join(" ")}: ${stderr || error.message}`));
      else resolve(stdout.trim());
    });
  });
}

let deploying = false;
let queued = false;

/**
 * Brings the checkout up to date and regenerates what the app renders.
 *
 * --ff-only so a force push upstream fails loudly here rather than silently
 * rewriting what is being served. Metro notices the changed files by itself, so
 * nothing restarts and the phone just reloads.
 */
async function deploy() {
  if (deploying) {
    queued = true;
    return;
  }
  deploying = true;
  try {
    await run("git", ["fetch", "--quiet", "origin", BRANCH]);
    const before = await run("git", ["rev-parse", "HEAD"]);
    await run("git", ["merge", "--ff-only", `origin/${BRANCH}`]);
    const after = await run("git", ["rev-parse", "HEAD"]);
    if (before === after) {
      log("already up to date at", after.slice(0, 8));
      return;
    }
    log("moved", before.slice(0, 8), "to", after.slice(0, 8));

    // The domain decides what a trip is, so the export has to follow the code.
    // A failure here is worth shouting about and not worth aborting for: the
    // previous trip.json is still valid and the screens still run.
    try {
      await run("./gradlew", ["exportTrip", "-q"]);
      log("trip exported");
    } catch (error) {
      log("trip export failed, keeping the last one:", error.message.split("\n")[0]);
    }

    // Dependencies only when they actually changed, because npm ci is slow.
    const changed = await run("git", ["diff", "--name-only", before, after]);
    if (changed.split("\n").some((file) => file === "app/package-lock.json")) {
      log("lockfile changed, installing");
      await run("npm", ["ci", "--prefix", "app", "--silent"]);
    }
    log("deployed");
  } catch (error) {
    log("deploy failed:", error.message);
  } finally {
    deploying = false;
    if (queued) {
      queued = false;
      deploy();
    }
  }
}

createServer((request, response) => {
  const done = (code, message) => {
    response.writeHead(code, { "content-type": "text/plain" });
    response.end(message + "\n");
  };

  if (request.method !== "POST") return done(405, "post only");

  const chunks = [];
  let size = 0;
  let aborted = false;

  request.on("data", (chunk) => {
    size += chunk.length;
    if (size > MAX_BODY) {
      aborted = true;
      done(413, "too large");
      request.destroy();
      return;
    }
    chunks.push(chunk);
  });

  request.on("end", () => {
    if (aborted) return;
    const body = Buffer.concat(chunks);

    if (!signatureIsGood(body, request.headers["x-hub-signature-256"])) {
      log("rejected a request with a bad signature");
      return done(401, "bad signature");
    }

    const event = request.headers["x-github-event"];
    if (event === "ping") return done(200, "pong");
    if (event !== "push") return done(200, `ignoring ${event}`);

    let payload;
    try {
      payload = JSON.parse(body.toString("utf8"));
    } catch {
      return done(400, "not json");
    }

    if (payload.ref !== `refs/heads/${BRANCH}`) {
      return done(200, `ignoring ${payload.ref}`);
    }

    // Answered before the work starts, because GitHub times a delivery out at
    // ten seconds and a Gradle run is longer than that.
    done(202, "deploying");
    deploy();
  });
}).listen(PORT, "127.0.0.1", () => {
  log(`listening on 127.0.0.1:${PORT}, serving ${REPO} on ${BRANCH}`);
});
