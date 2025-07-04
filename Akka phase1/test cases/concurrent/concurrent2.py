import sys
import random
from threading import Thread
import requests

from user import post_user
from wallet import put_wallet
from marketplace import post_order, get_product, test_get_product_stock
from utils import check_response_status_code, print_fail_message, print_pass_message

MARKETPLACE_SERVICE_URL = "http://localhost:8081"

# Global counter
successful_orders = 0

def bulk_order_thread(user_id, product_id, quantity):
    """Attempts to place bulk orders."""
    global successful_orders
    resp = post_order(user_id, [{"product_id": product_id, "quantity": quantity}])
    if resp.status_code == 201:
        successful_orders += 1
    elif resp.status_code == 400:
        print_fail_message("Order rejected due to insufficient stock.")
    else:
        print_fail_message("Unexpected order placement response!")

def main():
    try:
        user_id = 5001
        product_id = 107
        wallet_amount = 60000
        stock = 25  # Assuming initial stock is 30

        # Create user and fund wallet
        post_user(user_id, "Eve Bulk", "eve@bulk.com")
        put_wallet(user_id, "credit", wallet_amount)

        # Bulk order processing
        global successful_orders
        successful_orders = 0
        order_threads = [Thread(target=bulk_order_thread, args=(user_id, product_id, 2)) for _ in range(15)]
        for t in order_threads: t.start()
        for t in order_threads: t.join()

        # Check stock after bulk orders
        expected_final_stock = max(0, stock - (successful_orders * 2))
        resp = get_product(product_id)
        if not test_get_product_stock(product_id, resp, expected_stock=expected_final_stock):
            return False

        print_pass_message("Bulk Order Processing Test Passed.")
        return True

    except Exception as e:
        print_fail_message(f"Test crashed: {e}")
        return False

if __name__ == "__main__":
    if main():
        sys.exit(0)
    else:
        sys.exit(1)
