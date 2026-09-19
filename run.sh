#!/usr/bin/env bash
#
# Gets Linger onto your phone, from a computer or from the phone itself.
#
# Checks what it needs before it needs it, and says what is wrong in words
# rather than handing you a stack trace. Safe to run as many times as you like.

set -uo pipefail
cd "$(dirname "$0")"

bold=$'\033[1m'; dim=$'\033[2m'; green=$'\033[32m'; red=$'\033[31m'; amber=$'\033[33m'; off=$'\033[0m'
ok()   { printf '  %-32s %s\n' "$1" "${green}ok${off}"; }
warn() { printf '  %-32s %s\n' "$1" "${amber}$2${off}"; }
die()  { printf '  %-32s %s\n\n%s\n\n' "$1" "${red}no${off}" "$2"; exit 1; }

# Running inside Termux on the phone itself, rather than on a computer. The
# whole setup differs: no Java, and Expo Go connects over loopback rather than
# over the network, because the server and the app are the same device.
on_phone() { [ -n "${TERMUX_VERSION:-}" ] || [ -d /data/data/com.termux ]; }

printf '\n%sLinger%s\n' "$bold" "$off"
on_phone && printf '%sRunning on the phone itself%s\n\n' "$dim" "$off" || printf '%s------%s\n\n' "$dim" "$off"

# ------------------------------------------------------------- 0. the workspace
#
# Termux only, and both of these are one-time. Skipping them is not fatal today
# and costs you later: the server dies when the screen sleeps, and the app
# cannot reach a confirmation sitting in Downloads.

if on_phone; then
  # Keeps the CPU awake so Metro survives the screen going off. Released on the
  # way out, so it does not sit there draining the battery after you quit.
  if command -v termux-wake-lock >/dev/null; then
    termux-wake-lock
    trap 'command -v termux-wake-unlock >/dev/null && termux-wake-unlock' EXIT INT TERM
    ok "Wake lock"
  else
    warn "Wake lock" "unavailable, the server may sleep"
  fi

  if [ -d "$HOME/storage" ]; then
    ok "Shared storage"
  elif command -v termux-setup-storage >/dev/null; then
    warn "Shared storage" "not granted yet"
    printf '     %sRun termux-setup-storage and tap Allow. Needed later, when the\n     app reads confirmations out of Downloads.%s\n' "$dim" "$off"
  fi
fi

# ---------------------------------------------------------------- 1. the tools

if ! command -v node >/dev/null; then
  on_phone && die "Node.js" \
"Node is not installed. In Termux:

    pkg update && pkg install nodejs-lts

Take nodejs-lts rather than nodejs. The plain package tracks the newest
release, which Metro does not always support yet." \
  || die "Node.js" \
"Node is not installed. Get the LTS build from https://nodejs.org, then run
this script again."
fi

node_major=$(node --version | sed 's/v\([0-9]*\).*/\1/')
if [ "$node_major" -lt 20 ]; then
  die "Node.js $(node --version)" \
"Expo needs Node 20 or newer and you have $(node --version)."
fi
[ "$node_major" -ge 26 ] && warn "Node.js $(node --version)" "very new, LTS is safer" \
  || ok "Node.js $(node --version)"

# ------------------------------------------------- 2. the trip the app renders
#
# The Kotlin domain decides what a trip is. Regenerating keeps the phone honest
# when the model changes. Skipped on the phone: Termux has no JDK 21, and the
# committed export is the same file the build would produce.

if on_phone; then
  warn "Trip export" "skipped, no JDK here"
elif [ -x ./gradlew ] || command -v gradle >/dev/null; then
  gradle_cmd=$([ -x ./gradlew ] && echo ./gradlew || echo gradle)
  if $gradle_cmd exportTrip -q >/tmp/linger-export.log 2>&1; then
    ok "Trip exported"
  else
    warn "Trip export" "failed, using the committed one"
    printf '     %s(see /tmp/linger-export.log)%s\n' "$dim" "$off"
  fi
fi

[ -f app/assets/trip.json ] || die "Trip data" \
"app/assets/trip.json is missing. Run './gradlew exportTrip' on a machine with
Java and commit the result."

events=$(node -e "const t=require('./app/assets/trip.json');console.log(t.days.reduce((n,d)=>n+d.events.length,0))" 2>/dev/null)
ok "Trip data ($events events)"

# ------------------------------------------------------------ 3. the app itself

if [ ! -d app/node_modules ]; then
  if on_phone; then
    printf '  %-32s %s\n' "Dependencies" "${dim}installing, several minutes on a phone${off}"
  else
    printf '  %-32s %s\n' "Dependencies" "${dim}installing, about a minute${off}"
  fi
  (cd app && npm install --silent) || die "Dependencies" \
"npm install failed. Scroll up for the reason. On a phone the usual cause is
running out of memory: close other apps and try again."
fi
ok "Dependencies"

# ---------------------------------------------------------------- 4. launching

if on_phone; then
  url="exp://127.0.0.1:8081"
  cat <<NOTE

${bold}Opening in Expo Go${off}
  The server and the app are the same device, so there is no QR to scan and
  no wifi to match. Expo Go connects over loopback.

  If it does not open by itself, open Expo Go, choose ${bold}Enter URL manually${off}
  and type:

      ${bold}$url${off}

  ${dim}Keep Termux running. Swiping it away stops the server.
  Bundling takes a minute or two the first time.${off}

NOTE
  # Nudges Expo Go into the foreground pointed at the server. Best effort:
  # 'am' is not present in every Termux install, and the URL above is the
  # fallback that always works.
  if command -v am >/dev/null; then
    (sleep 12; am start -a android.intent.action.VIEW -d "$url" >/dev/null 2>&1) &
  fi
  cd app && exec npx expo start --localhost
fi

cat <<NOTE

${bold}On your phone${off}
  1. Install ${bold}Expo Go${off} from the Play Store, if you have not already.
  2. Put the phone on the ${bold}same wifi${off} as this computer.
  3. Open Expo Go and scan the QR code below.

  ${dim}Different networks, or wifi that blocks devices seeing each other?
  Stop this and run:  ./run.sh --tunnel${off}

NOTE

if [ "${1:-}" = "--tunnel" ]; then
  printf '%sStarting with a tunnel. Slower, and it works from anywhere.%s\n\n' "$dim" "$off"
  cd app && exec npx expo start --tunnel
fi

cd app && exec npx expo start
