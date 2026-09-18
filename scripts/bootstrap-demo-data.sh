#!/usr/bin/env bash
# Seeds 3 demo users with starting balances against a locally-running
# backend, so you can immediately start testing payments/history without
# manually signing accounts up first. Safe to re-run (falls back to logging
# in if a demo user already exists).
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
DEMO_PASSWORD="demo-password-123"
STARTING_BALANCE="750.00"

if ! command -v jq >/dev/null 2>&1; then
  echo "error: this script requires 'jq' to parse API responses." >&2
  echo "  install it, e.g.: apt install jq | brew install jq" >&2
  exit 1
fi

echo "Waiting for backend at ${API_BASE_URL}..."
for _ in $(seq 1 30); do
  if curl -s -o /dev/null "${API_BASE_URL}/api/users/lookup?paymentName=_health_check_"; then
    break
  fi
  sleep 1
done
if ! curl -s -o /dev/null "${API_BASE_URL}/api/users/lookup?paymentName=_health_check_"; then
  echo "error: backend at ${API_BASE_URL} did not come up in time." >&2
  echo "  start it first with: ./gradlew :backend:bootRun" >&2
  exit 1
fi
echo "Backend is up."
echo

# signup_or_login <firstName> <lastName> <paymentName>
# Prints "<token> <userId>" on success.
signup_or_login() {
  local first_name="$1" last_name="$2" payment_name="$3"
  local email="${payment_name}@example.com"
  local phone="555-01$(printf '%02d' $((RANDOM % 100)))"

  local response status
  response=$(curl -s -w '\n%{http_code}' -X POST "${API_BASE_URL}/api/users" \
    -H 'Content-Type: application/json' \
    -d "$(jq -n \
      --arg firstName "$first_name" --arg lastName "$last_name" \
      --arg email "$email" --arg phoneNumber "$phone" \
      --arg paymentName "$payment_name" --arg password "$DEMO_PASSWORD" \
      '{firstName:$firstName, lastName:$lastName, email:$email, phoneNumber:$phoneNumber, paymentName:$paymentName, password:$password}')")
  status=$(echo "$response" | tail -n1)
  body=$(echo "$response" | sed '$d')

  if [ "$status" = "201" ]; then
    echo "  [${payment_name}] created new account" >&2
  elif [ "$status" = "409" ]; then
    echo "  [${payment_name}] already exists, logging in instead" >&2
    body=$(curl -s -X POST "${API_BASE_URL}/api/auth/login" \
      -H 'Content-Type: application/json' \
      -d "$(jq -n --arg identifier "$payment_name" --arg password "$DEMO_PASSWORD" \
        '{identifier:$identifier, password:$password}')")
  else
    echo "error: unexpected status $status signing up ${payment_name}: $body" >&2
    exit 1
  fi

  local token user_id
  token=$(echo "$body" | jq -r '.token')
  user_id=$(echo "$body" | jq -r '.profile.id')
  echo "${token} ${user_id}"
}

# deposit <token> <userId> <amount>
deposit() {
  local token="$1" user_id="$2" amount="$3"
  curl -s -X POST "${API_BASE_URL}/api/accounts/me/deposit?userId=${user_id}" \
    -H "Authorization: Bearer ${token}" -H 'Content-Type: application/json' \
    -d "$(jq -n --argjson amount "$amount" '{amount:$amount}')" >/dev/null
}

echo "Creating demo users..."
read -r ALICE_TOKEN ALICE_ID <<< "$(signup_or_login Alice Anderson demo_alice)"
read -r BOB_TOKEN BOB_ID <<< "$(signup_or_login Bob Baker demo_bob)"
read -r CAROL_TOKEN CAROL_ID <<< "$(signup_or_login Carol Chen demo_carol)"

echo
echo "Depositing \$${STARTING_BALANCE} into each account..."
deposit "$ALICE_TOKEN" "$ALICE_ID" "$STARTING_BALANCE"
deposit "$BOB_TOKEN" "$BOB_ID" "$STARTING_BALANCE"
deposit "$CAROL_TOKEN" "$CAROL_ID" "$STARTING_BALANCE"

echo
echo "=================================================================="
echo " Demo accounts ready (password for all: ${DEMO_PASSWORD})"
echo "=================================================================="
printf "  %-12s %-6s %-10s %s\n" "paymentName" "id" "balance" "token"
printf "  %-12s %-6s %-10s %s\n" "demo_alice" "$ALICE_ID" "\$${STARTING_BALANCE}" "${ALICE_TOKEN:0:24}..."
printf "  %-12s %-6s %-10s %s\n" "demo_bob"   "$BOB_ID"   "\$${STARTING_BALANCE}" "${BOB_TOKEN:0:24}..."
printf "  %-12s %-6s %-10s %s\n" "demo_carol" "$CAROL_ID" "\$${STARTING_BALANCE}" "${CAROL_TOKEN:0:24}..."
echo
echo "Full tokens (for Authorization: Bearer <token>), if you want them:"
echo "  demo_alice: ${ALICE_TOKEN}"
echo "  demo_bob:   ${BOB_TOKEN}"
echo "  demo_carol: ${CAROL_TOKEN}"
echo
echo "Try it out (auth enforcement is currently relaxed, so ?userId= works"
echo "without a token too — see README's API section):"
echo
echo "  # Alice pays Bob \$25"
echo "  curl -X POST '${API_BASE_URL}/api/payments?userId=${ALICE_ID}' \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -d '{\"receiverPaymentName\":\"demo_bob\",\"amount\":25.00,\"note\":\"demo payment\"}'"
echo
echo "  # Bob's transaction history, last week"
echo "  curl \"${API_BASE_URL}/api/transactions/me?userId=${BOB_ID}&window=1W&page=0\""
echo
echo "  # Carol's balance"
echo "  curl \"${API_BASE_URL}/api/accounts/me?userId=${CAROL_ID}\""
echo "=================================================================="
