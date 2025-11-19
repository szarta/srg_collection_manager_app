#!/bin/bash
# Bundle card thumbnail images with the app

IMAGE_SOURCE_DIR="${IMAGE_SOURCE_DIR:-$HOME/data/srg_card_search_website/backend/app/images}"
ASSETS_DIR="app/src/main/assets"

echo "Bundling card images from: $IMAGE_SOURCE_DIR"
echo "Target directory: $ASSETS_DIR"

# Create assets thumbnails directory
mkdir -p "$ASSETS_DIR/thumbnails"

# Copy thumbnail images (preserving directory structure)
if [ -d "$IMAGE_SOURCE_DIR/thumbnails" ]; then
    echo "Copying thumbnails..."
    rsync -av --progress "$IMAGE_SOURCE_DIR/thumbnails/" "$ASSETS_DIR/thumbnails/"

    # Count files
    THUMB_COUNT=$(find "$ASSETS_DIR/thumbnails" -name "*.webp" | wc -l)
    echo "✓ Copied $THUMB_COUNT thumbnail images"

    # Show size
    SIZE=$(du -sh "$ASSETS_DIR/thumbnails" | cut -f1)
    echo "✓ Total size: $SIZE"
else
    echo "ERROR: Thumbnails directory not found at $IMAGE_SOURCE_DIR/thumbnails"
    exit 1
fi

echo ""
echo "Image bundling complete!"
echo "Note: Fullsize images will be downloaded from server on demand"
