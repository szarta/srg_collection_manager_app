#!/bin/bash
# Bundle mobile-optimized card images with the app

IMAGE_SOURCE_DIR="${IMAGE_SOURCE_DIR:-$HOME/data/srg_card_search_website/backend/app/images}"
ASSETS_DIR="app/src/main/assets"

echo "Bundling card images from: $IMAGE_SOURCE_DIR"
echo "Target directory: $ASSETS_DIR"

# Remove old thumbnails/fullsize if they exist
rm -rf "$ASSETS_DIR/thumbnails" "$ASSETS_DIR/fullsize"

# Create assets directory for mobile images
mkdir -p "$ASSETS_DIR/mobile"

# Copy mobile-optimized images (preserving directory structure)
if [ -d "$IMAGE_SOURCE_DIR/mobile" ]; then
    echo "Copying mobile-optimized images..."
    rsync -av --progress "$IMAGE_SOURCE_DIR/mobile/" "$ASSETS_DIR/mobile/"

    # Count files
    MOBILE_COUNT=$(find "$ASSETS_DIR/mobile" -name "*.webp" | wc -l)
    echo "✓ Copied $MOBILE_COUNT mobile images"

    # Show size
    SIZE=$(du -sh "$ASSETS_DIR/mobile" | cut -f1)
    echo "✓ Mobile images size: $SIZE"
else
    echo "ERROR: Mobile directory not found at $IMAGE_SOURCE_DIR/mobile"
    exit 1
fi

echo ""
echo "Image bundling complete!"
