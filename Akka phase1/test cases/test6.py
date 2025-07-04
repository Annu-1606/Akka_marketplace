import requests
import sys

userServiceURL = "http://localhost:8080"
marketplaceServiceURL = "http://localhost:8081"
walletServiceURL = "http://localhost:8082"

def main():
    name = "John Doe"
    email = "johndoe@mail.com"
    userId = 101
    productId = 104
    add_money_and_place_order(userId, name, email, productId)

def create_user(userId, name, email):
    new_user = {"id": userId, "name": name, "email": email}
    response = requests.post(userServiceURL + "/users", json=new_user)
    print(f"[DEBUG] Create User Response: {response.status_code}, {response.text}")
    return response

def create_wallet(user_id):
    response = requests.put(walletServiceURL + f"/wallets/{user_id}", json={"action": "credit", "amount": 0})
    print(f"[DEBUG] Create Wallet Response: {response.status_code}, {response.text}")

def get_wallet(user_id):
    response = requests.get(walletServiceURL + f"/wallets/{user_id}")
    print(f"[DEBUG] Get Wallet Response: {response.status_code}, {response.text}")
    return response

def update_wallet(user_id, action, amount):
    response = requests.put(walletServiceURL + f"/wallets/{user_id}", json={"action": action, "amount": amount})
    print(f"[DEBUG] Update Wallet Response: {response.status_code}, {response.text}")
    return response

def get_product_details(product_id):
    response = requests.get(marketplaceServiceURL + f"/products/{product_id}")
    print(f"[DEBUG] Get Product Details Response: {response.status_code}, {response.text}")
    return response   

def delete_order(order_id):
    response = requests.delete(f"{marketplaceServiceURL}/orders/{order_id}")
    print(f"[DEBUG] Delete Order Response: {response.status_code}, {response.text}")
    if response.status_code != 200:
        print("[ERROR] Wrong status code returned during delete order")
        sys.exit()
    return response

def delete_users():
    response = requests.delete(userServiceURL + f"/users")
    print(f"[DEBUG] Delete Users Response: {response.status_code}, {response.text}")

def add_money_and_place_order(userId, name, email, productId):
    try:
        print("[INFO] Starting test case execution...")
        delete_users()
        
        print("[INFO] Creating user...")
        new_user = create_user(userId, name, email)
        new_userid = new_user.json().get('id')
        
        if not new_userid:
            print("[ERROR] User creation failed!")
            return
        
        print("[INFO] Crediting wallet...")
        update_wallet(new_userid, "credit", 10000)
        
        print("[INFO] Fetching product details before ordering...")
        product_details_before_ordering = get_product_details(productId)
        
        print("[INFO] Checking initial wallet balance...")
        old_wallet_balance = get_wallet(new_userid).json().get('balance')
        
        print("[INFO] Placing order...")
        new_order = {"user_id": new_userid, "items": [{"product_id": productId, "quantity": 2}]}
        order_response = requests.post(marketplaceServiceURL + "/orders", json=new_order)
        print(f"[DEBUG] Order Response: {order_response.status_code}, {order_response.text}")
        #if order_response.status_code == 201:  # HTTP 201 means "Created"
        order_data = order_response.json()  # Convert response to dictionar
        order_id = order_data.get("order_id") 
        print(f"[DEBUG] Order Response: " ,order_data)
        print(f"[DEBUG] Order Response: " ,order_id)
         
        print("[INFO] Cancelling order...")
        delete_order(order_id)
        
        print("[INFO] Fetching product details after ordering...")
        product_details_after_ordering = get_product_details(productId)
        
        print("[INFO] Checking refund and stock consistency...")
        if product_details_after_ordering.json().get('stock_quantity') == product_details_before_ordering.json().get('stock_quantity') \
            and old_wallet_balance == get_wallet(new_userid).json().get('balance'):
            print("[SUCCESS] Test passed!")
        else:
            print("[FAIL] Test failed! Stock or wallet balance mismatch.")
    except Exception as e:
        print(f"[ERROR] Some Exception Occurred: {e}")

if __name__ == "__main__":
    main()
