# The box

Linger's dev server, running on a Hetzner box so that neither a laptop nor the
phone has to. You push, the box pulls and rebundles, your phone reloads. No
step in the middle belongs to you.

## Setting it up

On the box, once:

```
sudo bash -c "$(curl -fsSL https://raw.githubusercontent.com/S1rRV/linger-app/main/ops/install.sh)"
```

Or if it is already cloned:

```
sudo /opt/linger-app/ops/install.sh
```

It is idempotent. Run it again after a change and it repairs rather than
resets, and an existing webhook secret is kept rather than rotated, because
rotating it silently would break the GitHub side with no error pointing at the
cause.

It finishes by printing the two things only you can do: the webhook settings to
paste into GitHub, and the `exp://` address for Expo Go.

## What it installs

| | |
| --- | --- |
| `linger.service` | The Expo dev server, on the tailnet, restarts on failure and on boot |
| `linger-hook.service` | The deploy webhook, listening on loopback only |
| `/etc/linger/hook.env` | The webhook secret, `0640` and owned by `root:linger` |
| A Tailscale funnel | Public HTTPS for the webhook, with no port opened on the box |
| A `linger` system user | Owns the checkout. Nothing here runs as root |

## Why a funnel rather than an open port

The webhook has to be reachable by GitHub, which means reachable by everyone.
A funnel lets Tailscale terminate TLS in front of a listener bound to
`127.0.0.1`, so the box's firewall stays shut and there is no port to find by
scanning. The endpoint is still public, which is why the next section exists.

## How the webhook is defended

`ops/hook.js` assumes most of what arrives is hostile.

- **Every request is authenticated** by HMAC SHA-256 over the raw body, against
  the shared secret, compared with `timingSafeEqual`. GitHub is explicit that a
  plain `==` leaks the expected signature a byte at a time to a patient caller.
- **Nothing from the payload reaches a shell.** The body answers two questions,
  does the signature verify and did the push touch `main`, and after that the
  work is a fixed sequence of commands with fixed arguments through `execFile`.
- **Bodies over 1 MB are refused** without being buffered.
- **`git merge --ff-only`**, so a force push upstream fails loudly here rather
  than silently rewriting what is being served.
- **Runs as `linger`**, not root, under a systemd unit with `NoNewPrivileges`,
  `ProtectSystem=full` and `ProtectHome`.
- **Answers before it works.** GitHub times a delivery out at ten seconds and a
  Gradle run is longer than that.

Tested against a forged signature, a wrong secret, a truncated signature, a body
tampered with after signing, a `GET`, a push to another branch, and a valid push.

## Day to day

```
journalctl -fu linger          # the dev server
journalctl -fu linger-hook     # deploys as they land
systemctl restart linger       # if Metro gets confused
```

## The one thing this cannot do

It is still Expo Go, so reminders, share-sheet intake and calendar writes are
all out of reach. See `docs/research/expo-go-constraints.md`. The box makes the
loop fast; it does not change what Expo Go can run.
