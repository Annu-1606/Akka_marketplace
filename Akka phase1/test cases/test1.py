# test_sequential_orders.py
import requests
import sys

# Define service URLs
userServiceURL = "http://localhost:8080"
marketplaceServiceURL = "http://localhost:8081"
walletServiceURL = "http://localhost:8082"

# Function to create a user
def create_user(user_id, name, email):
    user_data = {"id": user_id, "name": name, "email": email}
    response = requests.post(userServiceURL + "/users", json=user_data)
    return response

# Function to update wallet balance
def update_wallet(user_id, action, amount):
    response = requests.put(walletServiceURL + f"/wallets/{user_id}", json={"action": action, "amount": amount})
    return response

# Function to place an order
def place_order(user_id, product_id, quantity):
    order_data = {"user_id": user_id, "items": [{"product_id": product_id, "quantity": quantity}]}
    response = requests.post(marketplaceServiceURL + "/orders", json=order_data)
    return response

# Function to get product details
def get_product_details(product_id):
    response = requests.get(marketplaceServiceURL + f"/products/{product_id}")
    return response

def main():
    user1, user2 = 301, 302  # Unique user IDs
    product_id = 104  # Product to be ordered
    order_quantity = 2  # Order quantity per user

    print("🔹 Deleting all users before starting the test.")
    requests.delete(userServiceURL + "/users")

    print("🔹 Fetching initial product stock before ordering.")
    product_before = get_product_details(product_id).json()
    print(f"📦 Initial Product Stock: {product_before['stock_quantity']}")

    print("🔹 Creating User 1 and funding wallet.")
    create_user(user1, "Alice", "alice@mail.com")
    update_wallet(user1, "credit", 5000)

    print("🔹 User 1 placing an order.")
    response1 = place_order(user1, product_id, order_quantity)
    print(f"🔹 User 1 Order Response: {response1.status_code}, {response1.text}")

    print("🔹 Fetching product stock after User 1 order.")
    product_after_user1 = get_product_details(product_id).json()
    print(f"📦 Product Stock after User 1: {product_after_user1['stock_quantity']}")

    print("🔹 Creating User 2 and funding wallet.")
    create_user(user2, "Bob", "bob@mail.com")
    update_wallet(user2, "credit", 5000)

    print("🔹 User 2 placing an order.")
    response2 = place_order(user2, product_id, order_quantity)
    print(f"🔹 User 2 Order Response: {response2.status_code}, {response2.text}")

    print("🔹 Fetching product stock after User 2 order.")
    product_after_user2 = get_product_details(product_id).json()
    print(f"📦 Product Stock after User 2: {product_after_user2['stock_quantity']}")

    # Test Case Validation
    if response1.status_code == 201 and response2.status_code == 201:
        print("✅ Test Passed: Both users successfully placed orders.")
    elif response1.status_code == 201 and response2.status_code == 400:
        print("✅ Test Passed: User 1 order succeeded, User 2 order failed due to insufficient stock.")
    else:
        print("❌ Test Failed: Unexpected behavior in order processing.")

if __name__ == "__main__":
    main()

