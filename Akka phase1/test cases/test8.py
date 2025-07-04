import sys
import random
from threading import Thread
import requests

from user import post_user
from wallet import put_wallet, get_wallet
from utils import check_response_status_code, print_fail_message, print_pass_message

WALLET_SERVICE_URL = "http://localhost:8082"

# Global variables to track total credits and debits
credited_amount = 0
debited_amount = 0

def credit_and_debit_thread(user_id, iterations=50):
    """
    Each thread randomly credits or debits the user's wallet.
    """
    global credited_amount, debited_amount
    for _ in range(iterations):
        action = random.choice(["credit", "debit"])
        amount = random.randint(10, 100)
        
        if action == "credit":
            resp = put_wallet(user_id, "credit", amount)
            if resp.status_code == 200:
                credited_amount += amount
        else:  # Debit action
            resp = put_wallet(user_id, "debit", amount)
            if resp.status_code == 200:
                debited_amount += amount

def main():
    try:
        # Step 1: Create a user
        user_id = 4001
        resp = post_user(user_id, "Dana Transactions", "dana@transactions.com")
        if not check_response_status_code(resp, 201):
            return False

        # Step 2: Give initial wallet balance
        initial_balance = 5000
        resp = put_wallet(user_id, "credit", initial_balance)
        if not check_response_status_code(resp, 200):
            return False

        # Step 3: Launch concurrent credit & debit operations
        global credited_amount, debited_amount
        credited_amount = 0
        debited_amount = 0

        thread_count = 3
        threads = []
        
        for _ in range(thread_count):
            t = Thread(target=credit_and_debit_thread, kwargs={"user_id": user_id, "iterations": 100})
            threads.append(t)
            t.start()

        for t in threads:
            t.join()

        # Step 4: Final check: Compute expected balance and verify
        expected_final_balance = initial_balance + credited_amount - debited_amount
        resp = get_wallet(user_id)
        if not check_response_status_code(resp, 200):
            return False
        
        wallet_data = resp.json()
        if wallet_data["balance"] != expected_final_balance:
            print_fail_message(f"Balance mismatch! Expected: {expected_final_balance}, Got: {wallet_data['balance']}")
            return False

        print_pass_message("Wallet stress test passed with concurrent transactions.")
        return True
    
    except Exception as e:
        print_fail_message(f"Test crashed: {e}")
        return False

if __name__ == "__main__":
    if main():
        sys.exit(0)
    else:
        sys.exit(1)
