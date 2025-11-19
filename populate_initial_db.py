#!/usr/bin/env python3
"""
Populate initial database with all cards from get-diced.com API
This will be bundled with the app for offline-first functionality
"""

import sqlite3
import requests
import time
import sys

API_BASE = "https://get-diced.com"
DB_FILE = "app/src/main/assets/cards_initial.db"
BATCH_SIZE = 100

def create_tables(conn):
    """Create the cards table matching the Android schema"""
    cursor = conn.cursor()

    cursor.execute("""
        CREATE TABLE IF NOT EXISTS cards (
            db_uuid TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            card_type TEXT NOT NULL,
            rules_text TEXT,
            errata_text TEXT,
            is_banned INTEGER NOT NULL DEFAULT 0,
            release_set TEXT,
            srg_url TEXT,
            srgpc_url TEXT,
            comments TEXT,
            tags TEXT,
            power INTEGER,
            agility INTEGER,
            strike INTEGER,
            submission INTEGER,
            grapple INTEGER,
            technique INTEGER,
            division TEXT,
            gender TEXT,
            deck_card_number INTEGER,
            atk_type TEXT,
            play_order TEXT,
            synced_at INTEGER NOT NULL
        )
    """)

    conn.commit()
    print("✓ Created cards table")

def fetch_cards_batch(offset, limit):
    """Fetch a batch of cards from the API"""
    url = f"{API_BASE}/cards?limit={limit}&offset={offset}"
    response = requests.get(url, timeout=30)
    response.raise_for_status()
    return response.json()

def insert_cards(conn, cards):
    """Insert cards into the database"""
    cursor = conn.cursor()
    sync_time = int(time.time() * 1000)

    for card in cards:
        tags_str = ",".join(card.get("tags", [])) if card.get("tags") else None

        cursor.execute("""
            INSERT OR REPLACE INTO cards VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
        """, (
            card["db_uuid"],
            card["name"],
            card["card_type"],
            card.get("rules_text"),
            card.get("errata_text"),
            1 if card.get("is_banned") else 0,
            card.get("release_set"),
            card.get("srg_url"),
            card.get("srgpc_url"),
            card.get("comments"),
            tags_str,
            card.get("power"),
            card.get("agility"),
            card.get("strike"),
            card.get("submission"),
            card.get("grapple"),
            card.get("technique"),
            card.get("division"),
            card.get("gender"),
            card.get("deck_card_number"),
            card.get("atk_type"),
            card.get("play_order"),
            sync_time
        ))

    conn.commit()

def main():
    print("Downloading cards from get-diced.com...")
    print(f"Output: {DB_FILE}\n")

    # Create database
    conn = sqlite3.connect(DB_FILE)
    create_tables(conn)

    # Fetch first batch to get total count
    data = fetch_cards_batch(0, BATCH_SIZE)
    total_count = data["total_count"]

    print(f"Total cards to download: {total_count}")
    print("=" * 50)

    offset = 0
    total_downloaded = 0

    while offset < total_count:
        data = fetch_cards_batch(offset, BATCH_SIZE)
        cards = data["items"]

        if not cards:
            break

        insert_cards(conn, cards)
        total_downloaded += len(cards)

        progress = (total_downloaded / total_count) * 100
        print(f"Progress: {total_downloaded}/{total_count} ({progress:.1f}%)")

        offset += BATCH_SIZE
        time.sleep(0.1)  # Be nice to the API

    conn.close()

    print("=" * 50)
    print(f"✓ Successfully downloaded {total_downloaded} cards")
    print(f"✓ Database saved to: {DB_FILE}")

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\nInterrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n✗ Error: {e}")
        sys.exit(1)
