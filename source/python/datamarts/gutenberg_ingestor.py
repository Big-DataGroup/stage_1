from datetime import datetime
import os
import sys
import time
import requests

START_MARKER = "*** START OF THE PROJECT GUTENBERG EBOOK"
END_MARKER = "*** END OF THE PROJECT GUTENBERG EBOOK"


class GutenbergIngestor:
    def __init__(self, base_output_dir="../datalake"):
        self.base_output_dir = base_output_dir

    def resolve_datalake_path(self, book_id, strategy):
        strategy_lower = strategy.lower()
        if strategy_lower == "by_book":
            return os.path.join(self.base_output_dir, "by_book", str(book_id))
        elif strategy_lower == "by_batch":
            lower_bound = (book_id // 1000) * 1000
            upper_bound = lower_bound + 999
            batch_name = f"{lower_bound}-{upper_bound}"
            return os.path.join(self.base_output_dir, "by_batch", batch_name)
        elif strategy_lower == "by_time":
            now = datetime.now()
            date_folder = now.strftime("%Y%m%d")
            hour_folder = now.strftime("%H")
            return os.path.join(self.base_output_dir, "by_time", date_folder, hour_folder)
        else:
            raise ValueError(f"Unknown datalake strategy: {strategy}")

    def download_book(self, book_id, strategy):
        output_dir = self.resolve_datalake_path(book_id, strategy)
        body_path = os.path.join(output_dir, f"{book_id}.body.txt")
        header_path = os.path.join(output_dir, f"{book_id}.header.txt")

        if os.path.exists(body_path) and os.path.exists(header_path):
            print(f"Skipping book {book_id}: Files already exist (Recovery Mode)")
            return True

        url = f"https://www.gutenberg.org/cache/epub/{book_id}/pg{book_id}.txt"

        try:
            response = requests.get(url, allow_redirects=True, timeout=10)

            if response.status_code != 200:
                print(f"Error HTTP {response.status_code} for the book ID: {book_id}", file=sys.stderr)
                return False

            text = response.text

            if START_MARKER not in text or END_MARKER not in text:
                print(f"Book not found or markers missing: {book_id}", file=sys.stderr)
                return False

            parts1 = text.split(START_MARKER, 1)
            header = parts1[0]

            parts2 = parts1[1].split(END_MARKER, 1)
            body = parts2[0]

            os.makedirs(output_dir, exist_ok=True)

            with open(body_path, "w", encoding="utf-8") as f:
                f.write(body.strip())
            with open(header_path, "w", encoding="utf-8") as f:
                f.write(header.strip())

            print(f"Book {book_id} saved using [{strategy}]")
            return True

        except Exception as e:
            print(f"Network or disk error while processing the book {book_id}: {e}", file=sys.stderr)
            return False

    def download_batch(self, book_ids, strategy):
        print("=== STARTING BATCH DOWNLOAD (Python) ===")
        success_count = 0

        for book_id in book_ids:
            success = self.download_book(book_id, strategy)
            if success:
                success_count += 1

            time.sleep(0.2)

        print(f"=== BATCH COMPLETED: {success_count}/{len(book_ids)} SUCCESSFUL ===")


if __name__ == "__main__":
    start_id = 1
    end_id = 1000
    sample_books = list(range(start_id, end_id + 1))
    strategies = ["by_book"]

    print(f"--- Generating Sample Dataset ({len(sample_books)} books) ---")

    ingestor = GutenbergIngestor(base_output_dir="../datalake")

    for strategy in strategies:
        print(f"\n-> Executing strategy: {strategy}")
        ingestor.download_batch(sample_books, strategy)