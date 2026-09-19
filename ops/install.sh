#!/usr/bin/env bash
#
# Turns a Hetzner box into the always-on dev server.
#
# Run it with sudo, on the box, as many times as you like. It checks before it
# changes anything and leaves an existing secret alone, so a second run is a
# repair rather than a reset.

set -euo pipefail

REPO_URL="${LINGER_REPO_URL:-https://github.com/S1rRV/linger-app}"
REPO_DIR="${LINGER_REPO:-/opt/linger-app}"
HOOK_PORT="${LINGER_HOOK_PORT:-9871}"
ENV_FILE=/etc/linger/hook.env

bold=$'\033[1m'; dim=$'\033[2m'; green=$'\033[32m'; amber=$'\033[33m'; off=$'\033[0m'
ok()   { printf '  %-30s %s\n' "$1" "${green}ok${off}"; }
warn() { printf '  %-30s %s\n' "$1" "${amber}$2${off}"; }
die()  { printf '\n%s\n\n' "$1" >&2; exit 1; }

[ "$(id -u)" -eq 0 ] || die "Run this with sudo. It installs systemd units and writes to /opt."

printf '\n%sLinger, on this box%s\n\n' "$bold" "$off"

# ------------------------------------------------------------------ 1. the tools

command -v node >/dev/null || die "Node is not installed. On Debian or Ubuntu:

    curl -fsSL https://deb.nodesource.com/setup_lts.x | bash -
    apt-get install -y nodejs"
node_major=$(node --version | sed 's/v\([0-9]*\).*/\1/')
[ "$node_major" -ge 20 ] || die "Expo needs Node 20 or newer, and this box has $(node --version)."
ok "Node $(node --version)"

command -v git >/dev/null || die "git is not installed. apt-get install -y git"
ok "git"

command -v java >/dev/null && ok "Java $(java -version 2>&1 | grep -m1 'version \"' | sed 's/.*"\([^"]*\)".*/\1/')" \
  || warn "Java" "missing, the trip export will be skipped"

command -v tailscale >/dev/null || die "Tailscale is not installed. See https://tailscale.com/download"
tailscale ip -4 >/dev/null 2>&1 || die "Tailscale is installed but not connected. Run: tailscale up"
ok "Tailscale $(tailscale ip -4 | head -1)"

# ------------------------------------------------------------------- 2. the user
#
# Its own account rather than root. Everything below runs code that arrives from
# the internet, and it has no business being able to touch the rest of the box.

if ! id linger >/dev/null 2>&1; then
  useradd --system --create-home --home-dir /var/lib/linger --shell /usr/sbin/nologin linger
fi
ok "User linger"

# ---------------------------------------------------------------- 3. the checkout

if [ -d "$REPO_DIR/.git" ]; then
  git -C "$REPO_DIR" remote set-url origin "$REPO_URL"
  ok "Checkout $REPO_DIR"
else
  git clone --quiet "$REPO_URL" "$REPO_DIR"
  ok "Cloned to $REPO_DIR"
fi
chown -R linger:linger "$REPO_DIR"
# Git refuses to work in a tree owned by someone else unless told it is fine.
sudo -u linger git config --global --add safe.directory "$REPO_DIR" 2>/dev/null || true

printf '  %-30s %s\n' "Dependencies" "${dim}installing${off}"
sudo -u linger npm ci --prefix "$REPO_DIR/app" --silent
ok "Dependencies"

# ------------------------------------------------------------------ 4. the secret
#
# Generated here and never regenerated, because rotating it silently would
# break the GitHub side with no error that points at the cause.

mkdir -p /etc/linger
if [ -f "$ENV_FILE" ] && grep -q LINGER_HOOK_SECRET "$ENV_FILE"; then
  secret=$(grep LINGER_HOOK_SECRET "$ENV_FILE" | cut -d= -f2-)
  ok "Webhook secret (kept)"
else
  secret=$(head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n')
  cat > "$ENV_FILE" <<ENV
LINGER_HOOK_SECRET=$secret
LINGER_HOOK_PORT=$HOOK_PORT
LINGER_REPO=$REPO_DIR
LINGER_BRANCH=main
ENV
  ok "Webhook secret (new)"
fi
chown root:linger "$ENV_FILE"
chmod 640 "$ENV_FILE"

# ----------------------------------------------------------------- 5. the services

install -m 644 "$REPO_DIR/ops/linger.service" /etc/systemd/system/linger.service
install -m 644 "$REPO_DIR/ops/linger-hook.service" /etc/systemd/system/linger-hook.service
systemctl daemon-reload
systemctl enable --now linger.service linger-hook.service >/dev/null
ok "Services enabled"

# ------------------------------------------------------------------- 6. the funnel
#
# A funnel rather than an open firewall port: the box keeps every port shut and
# Tailscale terminates TLS in front of a listener bound to loopback.

funnel_url=""
if tailscale funnel --bg "http://127.0.0.1:$HOOK_PORT" >/dev/null 2>&1; then
  funnel_url=$(tailscale funnel status 2>/dev/null | grep -o 'https://[^ ]*' | head -1)
  ok "Funnel"
else
  warn "Funnel" "could not start it"
fi

# --------------------------------------------------------------------- 7. what now

cat <<DONE

${bold}Done.${off} Two things left, both on the web.

${bold}1. Point GitHub at the box${off}
   Settings, Webhooks, Add webhook on the repository:

     Payload URL   ${bold}${funnel_url:-https://<this-box>.ts.net/}${off}
     Content type  ${bold}application/json${off}
     Secret        ${bold}$secret${off}
     Events        ${bold}Just the push event${off}

${bold}2. Point your phone at the box${off}
   Open Expo Go, Enter URL manually:

     ${bold}exp://$(tailscale ip -4 | head -1):8081${off}

   Tailscale has to be on on the phone. Nothing else.

${dim}Watching it:   journalctl -fu linger
Watching deploys: journalctl -fu linger-hook
Restarting:    systemctl restart linger${off}

DONE
