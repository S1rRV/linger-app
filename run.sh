#!/usr/bin/env bash
#
# Gets Linger onto your phone.
#
# Checks what it needs before it needs it, and says what is wrong in words
# rather than handing you a stack trace. Safe to run as many times as you like.

set -uo pipefail
cd "$(dirname "$0")"

bold=$'\033[1m'; dim=$'\033[2m'; green=$'\033[32m'; red=$'\033[31m'; amber=$'\033[33m'; off=$'\033[0m'
ok()   { printf '  %-34s %s\n' "$1" "${green}ok${off}"; }
warn() { printf '  %-34s %s\n' "$1" "${amber}$2${off}"; }
die()  { printf '  %-34s %s\n\n%s\n\n' "$1" "${red}no${off}" "$2"; exit 1; }

printf '\n%sLinger%s\n%s------%s\n\n' "$bold" "$off" "$dim" "$off"

# ---------------------------------------------------------------- 1. the tools

command -v node >/dev/null || die "Node.js" \
"Node is not installed. Get it from https://nodejs.org (take the LTS build),
then run this script again."

node_major=$(node --version | sed 's/v\([0-9]*\).*/\1/')
if [ "$node_major" -lt 20 ]; then
  die "Node.js $(node --version)" \
"Expo needs Node 20 or newer and you have $(node --version).
Upgrade from https://nodejs.org and run this again."
fi
ok "Node.js $(node --version)"

# Grepped for the quoted version rather than taking the first line, because
# JAVA_TOOL_OPTIONS prints a banner ahead of it on some machines.
java_version=$(java -version 2>&1 | grep -m1 'version "' | sed 's/.*"\([^"]*\)".*/\1/')
if [ -n "$java_version" ]; then ok "Java $java_version"; else warn "Java" "missing, skipping the trip export"; fi

# ------------------------------------------------- 2. the trip the app renders
#
# The Kotlin domain decides what a trip is. This regenerates what the app draws,
# so a change to the model shows up on your phone rather than going stale here.

if command -v java >/dev/null && [ -x ./gradlew ] || command -v gradle >/dev/null; then
  gradle_cmd=$([ -x ./gradlew ] && echo ./gradlew || echo gradle)
  if $gradle_cmd exportTrip -q >/tmp/linger-export.log 2>&1; then
    ok "Trip exported"
  else
    warn "Trip export" "failed, using the committed one"
    printf '     %s(see /tmp/linger-export.log)%s\n' "$dim" "$off"
  fi
fi

[ -f app/assets/trip.json ] || die "Trip data" \
"app/assets/trip.json is missing and could not be regenerated.
Run 'gradle exportTrip' and read the error it gives you."

events=$(node -e "const t=require('./app/assets/trip.json');console.log(t.days.reduce((n,d)=>n+d.events.length,0))" 2>/dev/null)
ok "Trip data ($events events)"

# ------------------------------------------------------------ 3. the app itself

if [ ! -d app/node_modules ]; then
  printf '  %-34s %s\n' "Dependencies" "${dim}installing, this takes a minute${off}"
  (cd app && npm install --silent) || die "Dependencies" \
"npm install failed. Scroll up for the reason. The usual causes are no internet
or a corporate proxy blocking the npm registry."
fi
ok "Dependencies"

# ---------------------------------------------------------------- 4. your phone

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
