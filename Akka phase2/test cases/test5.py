#checking double cancellation
import requests

# Service Endpoints
userServiceURL = "http://localhost:8080"
marketplaceServiceURL = "http://localhost:8081"
walletServiceURL = "http://localhost:8082"

def create_user(user_id, name, email):
    """Creates a new user."""
    user_data = {"id": user_id, "name": name, "email": email}
    response = requests.post(userServiceURL + "/users", json=user_data)
    if response.status_code != 201:
        print(f"❌ Failed to create user: {response.status_code}, {response.text}")
    return response

def update_wallet(user_id, action, amount):
    """Credits or debits wallet balance."""
    response = requests.put(walletServiceURL + f"/wallets/{user_id}", json={"action": action, "amount": amount})
    if response.status_code != 200:
        print(f"❌ Wallet update failed: {response.status_code}, {response.text}")
    return response

def get_wallet_balance(user_id):
    """Fetches wallet balance."""
    response = requests.get(walletServiceURL + f"/wallets/{user_id}")
    return response.json().get("balance", 0) if response.status_code == 200 else None

def place_order(user_id, product_id, quantity):
    """Places an order."""
    order_data = {"user_id": user_id, "items": [{"product_id": product_id, "quantity": quantity}]}
    response = requests.post(marketplaceServiceURL + "/orders", json=order_data)
    if response.status_code != 201:
        print(f"❌ Order placement failed: {response.status_code}, {response.text}")
    return response

def cancel_order(order_id):
    """Cancels an order."""
    response = requests.delete(f"{marketplaceServiceURL}/orders/{order_id}")
    return response

def delete_all_users():
    """Deletes all users before testing to ensure a clean state."""
    requests.delete(userServiceURL + "/users")

def main():
    user_id = 105
    product_id = 107
    wallet_amount = 5000
    order_quantity = 1

    print("🔹 Deleting all users before starting the test.")
    delete_all_users()

    print("🔹 Creating a new user and funding their wallet.")
    create_user(user_id, "Alice", "alice@mail.com")
    update_wallet(user_id, "credit", wallet_amount)

    print("🔹 Placing an order.")
    order_response = place_order(user_id, product_id, order_quantity)
    if order_response.status_code != 201:
        print("❌ Order placement failed unexpectedly.")
        return

    order_id = order_response.json().get("order_id")
    if not order_id:
        print("❌ Failed to extract order ID from response.")
        return

    print(f"🔹 Order ID {order_id} placed successfully.")

    print("🔹 Cancelling the order for the first time.")
    cancel_response_1 = cancel_order(order_id)
    print(f"🔹 First cancellation response: {cancel_response_1.status_code}, {cancel_response_1.text}")

    print("🔹 Attempting to cancel the order again.")
    cancel_response_2 = cancel_order(order_id)
    print(f"🔹 Second cancellation response: {cancel_response_2.status_code}, {cancel_response_2.text}")

    # Test Validation
    if cancel_response_1.status_code == 200 and cancel_response_2.status_code in [400, 404]:
        print("✅ Test Passed: Order was correctly cancelled once and rejected the second time.")
    else:
        print("❌ Test Failed: Incorrect cancellation handling.")

if __name__ == "__main__":
    main()

