import sys
import random
from threading import Thread
import requests

from user import post_user, delete_user, get_user
from utils import check_response_status_code, print_fail_message, print_pass_message

USER_SERVICE_URL = "http://localhost:8080"

# Global counter for successful deletions
successful_deletions = 0

def delete_user_thread(user_id, attempts=5):
    """
    Each thread attempts to delete the same user multiple times.
    """
    global successful_deletions
    for _ in range(attempts):
        resp = delete_user(user_id)
        
        if resp.status_code == 200:
            successful_deletions += 1
        elif resp.status_code == 404:
            print_pass_message(f"User {user_id} already deleted.")
        else:
            print_fail_message(f"Unexpected response {resp.status_code} for DELETE /users/{user_id}")

def main():
    try:
        # Step 1: Create a user
        user_id = 3001
        resp = post_user(user_id, "Charlie Resilience", "charlie@resilience.com")
        if not check_response_status_code(resp, 201):
            return False
        
        # Step 2: Launch concurrency threads to delete the user
        global successful_deletions
        successful_deletions = 0

        thread_count = 3
        threads = []

        for _ in range(thread_count):
            t = Thread(target=delete_user_thread, kwargs={"user_id": user_id, "attempts": 5})
            threads.append(t)
            t.start()

        for t in threads:
            t.join()

        # Step 3: Final check: The user should not exist anymore
        resp = get_user(user_id)
        if not check_response_status_code(resp, 404):
            return False

        print_pass_message(f"User deletion test passed with {successful_deletions} successful deletions.")
        return True
    
    except Exception as e:
        print_fail_message(f"Test crashed: {e}")
        return False

if __name__ == "__main__":
    if main():
        sys.exit(0)
    else:
        sys.exit(1)
