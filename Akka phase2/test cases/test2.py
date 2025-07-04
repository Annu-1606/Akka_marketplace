# test_insufficient_balance.py
import requests
import sys

userServiceURL = "http://localhost:8080"
marketplaceServiceURL = "http://localhost:8081"
walletServiceURL = "http://localhost:8082"

def create_user(user_id, name, email):
    user_data = {"id": user_id, "name": name, "email": email}
    return requests.post(userServiceURL + "/users", json=user_data)

def update_wallet(user_id, action, amount):
    return requests.put(walletServiceURL + f"/wallets/{user_id}", json={"action": action, "amount": amount})

def place_order(user_id, product_id, quantity):
    order_data = {"user_id": user_id, "items": [{"product_id": product_id, "quantity": quantity}]}
    return requests.post(marketplaceServiceURL + "/orders", json=order_data)

def main():
    user_id = 104
    product_id = 105
    order_quantity = 1
    print("🔹 Deleting all users before starting the test.")
    requests.delete(userServiceURL + "/users")

    print("🔹 Creating a user with insufficient balance.")
    create_user(user_id, "Charlie", "charlie@mail.com")
    update_wallet(user_id, "credit", 10)  # Only 10 credits, not enough for a product

    print("🔹 Attempting to place an order with insufficient balance.")
    response = place_order(user_id, product_id, order_quantity)

    print(f"🔹 Order Response: {response.status_code}, {response.text}")

    if response.status_code == 400:
        print("✅ Test Passed: Order was correctly rejected due to insufficient funds.")
    else:
        print("❌ Test Failed: Order was incorrectly processed.")

if __name__ == "__main__":
    main()

