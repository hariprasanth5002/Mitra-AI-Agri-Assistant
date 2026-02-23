# Publishing Mitra to GitHub - Instructions

## What I've Done:
1. ✅ Updated `.gitignore` to exclude:
   - Python virtual environments (venv/, env/, .venv/)
   - Large model files (*.keras, *.h5, *.pt, etc.)
   - Node modules (node_modules/)
   - Build artifacts (target/, dist/, build/)
   - IDE files (.idea/, .vscode/)
   - Upload folders
   
2. ✅ Removed `Image/mobilenet_model.keras` from Git tracking (27 MB file)
3. ✅ Committed changes to `clean-main` branch

## Next Steps to Publish:

### Option 1: Push with increased buffer (Recommended)
```bash
cd "c:\Users\LENOVO\Documents\Mitra – AI Agri Assistant"
git config http.postBuffer 524288000
git push origin clean-main
```

### Option 2: If push still fails, try smaller commits
```bash
# Handle the submodule issue first
cd Weather/weather-advisory-frontend
git add .
git commit -m "Update weather frontend"
cd ../..

# Then push main repo
git add .
git commit -m "Sync weather frontend submodule"
git push origin clean-main
```

### Option 3: Push to main branch instead
```bash
git checkout -b main
git push origin main
```

## Important Notes:

### About the Model File (mobilenet_model.keras)
- This file is now IGNORED by Git
- You need to document how users should obtain it
- Options:
  1. Upload to GitHub Releases (recommended)
  2. Use Git LFS (Large File Storage)
  3. Provide download link in README

### Add to README.md:
```markdown
## Getting the Model File

The MobileNetV2 model file (`mobilenet_model.keras`, ~27 MB) is not included in the repository.

**Download it from:**
- [GitHub Releases](https://github.com/hariprasanth5002/Mitra-AI-Agri-Assistant01/releases)
- Or train your own using the PlantVillage dataset

**Installation:**
1. Download `mobilenet_model.keras`
2. Place it in the `Image/` folder
3. Verify path in `Image/app.py`
```

## Verify Before Publishing:
```bash
# Check what will be pushed
git status

# Check file sizes
git ls-files | findstr /v "venv node_modules .keras"

# Verify .gitignore is working
git check-ignore -v venv/ node_modules/ Image/mobilenet_model.keras
```

## After Successful Push:
1. Go to https://github.com/hariprasanth5002/Mitra-AI-Agri-Assistant01
2. Create a Release and upload `mobilenet_model.keras`
3. Update README with download instructions
4. Set `clean-main` or `main` as default branch in repo settings
